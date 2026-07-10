package com.bricopro.task.service;

import com.bricopro.notification.service.CommunicationService;
import com.bricopro.task.entity.Task;
import com.bricopro.task.entity.Task.CancelledBy;
import com.bricopro.task.entity.Task.TaskStatus;
import com.bricopro.task.repository.TaskRepository;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.WorkerProfile;
import com.bricopro.user.repository.UserRepository;
import com.bricopro.user.repository.WorkerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CancellationService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final WorkerProfileRepository workerProfileRepository;
    private final CommunicationService communicationService;

    @Transactional
    public CancellationResult cancel(Task task, Long actorId, String reason) {
        CancelledBy cancelledBy = actorId.equals(task.getClient().getId())
                ? CancelledBy.CLIENT : CancelledBy.WORKER;

        boolean wasConfirmed = task.getStatus() == TaskStatus.CONFIRMED
                || task.getStatus() == TaskStatus.STARTED;

        task.setStatus(TaskStatus.CANCELLED);
        task.setCancelledBy(cancelledBy);
        task.setCancellationReason(reason);
        taskRepository.save(task);

        BigDecimal penalty = BigDecimal.ZERO;
        boolean penaltyApplied = false;
        String penaltyReason = "";
        boolean forgiven = false;
        boolean visibilityReduction = false;
        boolean badgeLost = false;
        int reputationLoss = 0;

        if (cancelledBy == CancelledBy.CLIENT) {
            User client = task.getClient();
            int monthCount = client.getCancellationCountThisMonth();
            LocalDateTime scheduledDateTime = LocalDateTime.of(task.getScheduledDate(), task.getScheduledStart());
            long hoursUntil = java.time.Duration.between(LocalDateTime.now(), scheduledDateTime).toHours();

            if (hoursUntil > 2) {
                forgiven = true;
                penaltyReason = "Annulation gratuite (>2h)";
            } else if (hoursUntil >= 0.5) {
                if (monthCount == 0) {
                    forgiven = true;
                    penaltyReason = "Premier oubli – aucun frais";
                } else {
                    penalty = BigDecimal.valueOf(5);
                    penaltyApplied = true;
                    penaltyReason = "Crédit 5 MAD perdu";
                }
            } else {
                BigDecimal base = task.getAgreedPrice() != null ? task.getAgreedPrice() : BigDecimal.valueOf(100);
                penalty = base.multiply(BigDecimal.valueOf(0.15)).min(BigDecimal.valueOf(20)).setScale(2, RoundingMode.HALF_UP);
                penaltyApplied = true;
                penaltyReason = "Pénalité de dernière minute";
            }

            if (!forgiven) {
                client.setCancellationCountThisMonth(monthCount + 1);
                reputationLoss = 5;
                client.setReliabilityScore(Math.max(0, client.getReliabilityScore() - reputationLoss));
                userRepository.save(client);
            }

        } else if (wasConfirmed && task.getWorker() != null) {
            User worker = task.getWorker();
            WorkerProfile profile = workerProfileRepository.findByUserId(worker.getId()).orElse(null);
            int monthCount = worker.getCancellationCountThisMonth();

            if (monthCount == 0) {
                forgiven = true;
                penaltyReason = "Premier avertissement – pas de pénalité";
            } else if (monthCount == 1 || monthCount == 2) {
                visibilityReduction = true;
                penaltyReason = "Visibilité réduite 48h";
                if (profile != null) {
                    profile.setVisibilityReductionUntil(LocalDateTime.now().plusHours(48));
                    workerProfileRepository.save(profile);
                }
                reputationLoss = 10;
            } else if (monthCount >= 3) {
                badgeLost = true;
                penaltyReason = "Perte du badge vérifié après annulation";
                if (profile != null) {
                    profile.setVerifiedBadge(false);
                    workerProfileRepository.save(profile);
                }
                reputationLoss = 20;
            }

            worker.setCancellationCountThisMonth(monthCount + 1);
            worker.setReliabilityScore(Math.max(0, worker.getReliabilityScore() - reputationLoss));
            userRepository.save(worker);
        }

        if (penaltyApplied && penalty.compareTo(BigDecimal.ZERO) > 0) {
            communicationService.sendEmail(
                    task.getClient().getEmail(),
                    "BricoPro — Frais d'annulation",
                    "Vous avez annulé la mission \"" + task.getTitle() + "\". Frais appliqués : " + penalty + " MAD. Motif : " + penaltyReason
            );
        }

        return new CancellationResult(task.getId(), cancelledBy.name(), penaltyApplied, penalty, penaltyReason,
                forgiven, visibilityReduction, badgeLost, reputationLoss);
    }

    public record CancellationResult(Long taskId, String cancelledBy, boolean penaltyApplied, BigDecimal penaltyAmount,
                                     String penaltyReason, boolean forgiven, boolean visibilityReduction,
                                     boolean badgeLost, int reputationLoss) {
    }
}