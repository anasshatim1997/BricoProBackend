package com.bricopro.subscription.service;

import com.bricopro.subscription.entity.WorkerSubscription;
import com.bricopro.subscription.repository.WorkerSubscriptionRepository;
import com.bricopro.user.entity.User;
import com.bricopro.user.repository.UserRepository;
import com.bricopro.user.repository.WorkerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WorkerSubscriptionService {

    private static final Map<WorkerSubscription.Plan, Integer> JOB_LIMITS = Map.of(
            WorkerSubscription.Plan.FREE,       3,
            WorkerSubscription.Plan.PREMIUM,    Integer.MAX_VALUE,
            WorkerSubscription.Plan.ENTERPRISE, Integer.MAX_VALUE
    );

    private static final Map<WorkerSubscription.Plan, BigDecimal> PRICES = Map.of(
            WorkerSubscription.Plan.FREE,       BigDecimal.ZERO,
            WorkerSubscription.Plan.PREMIUM,    new BigDecimal("99"),
            WorkerSubscription.Plan.ENTERPRISE, new BigDecimal("249")
    );

    private final WorkerSubscriptionRepository subRepository;
    private final WorkerProfileRepository      workerProfileRepository;
    private final UserRepository               userRepository;

    public WorkerSubscription getCurrentPlan(Long workerId) {
        return subRepository.findTopByWorkerIdAndSubStatusOrderByCreatedAtDesc(
                        workerId, WorkerSubscription.SubStatus.ACTIVE)
                .orElseGet(() -> WorkerSubscription.builder()
                        .plan(WorkerSubscription.Plan.FREE)
                        .subStatus(WorkerSubscription.SubStatus.ACTIVE)
                        .build());
    }

    public boolean canAcceptJob(Long workerId, int currentMonthJobs) {
        WorkerSubscription sub = getCurrentPlan(workerId);
        if (sub.getExpiresAt() != null && LocalDateTime.now().isAfter(sub.getExpiresAt())) {
            return currentMonthJobs < JOB_LIMITS.get(WorkerSubscription.Plan.FREE);
        }
        return currentMonthJobs < JOB_LIMITS.get(sub.getPlan());
    }

    @Transactional
    public WorkerSubscription upgrade(Long workerId, WorkerSubscription.Plan plan) {
        User worker = userRepository.findById(workerId)
                .orElseThrow(() -> new IllegalArgumentException("Worker not found"));

        subRepository.findTopByWorkerIdAndSubStatusOrderByCreatedAtDesc(
                workerId, WorkerSubscription.SubStatus.ACTIVE)
                .ifPresent(s -> {
                    s.setSubStatus(WorkerSubscription.SubStatus.CANCELLED);
                    subRepository.save(s);
                });

        WorkerSubscription sub = WorkerSubscription.builder()
                .worker(worker)
                .plan(plan)
                .subStatus(WorkerSubscription.SubStatus.ACTIVE)
                .startedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMonths(1))
                .amountPaid(PRICES.get(plan))
                .paymentReference("SUB-" + workerId + "-" + System.currentTimeMillis())
                .build();
        sub = subRepository.save(sub);

        workerProfileRepository.findByUserId(workerId).ifPresent(wp -> {
            wp.setPremium(plan != WorkerSubscription.Plan.FREE);
            workerProfileRepository.save(wp);
        });

        return sub;
    }

    public Map<String, Object> getPlansInfo() {
        return Map.of(
                "FREE",       Map.of("price", 0,   "jobsPerMonth", 3,  "features", new String[]{"3 missions/mois", "Profil basique"}),
                "PREMIUM",    Map.of("price", 99,  "jobsPerMonth", -1, "features", new String[]{"Missions illimitées", "Badge Premium", "Priorité dans la recherche", "Statistiques avancées"}),
                "ENTERPRISE", Map.of("price", 249, "jobsPerMonth", -1, "features", new String[]{"Tout Premium", "Support dédié", "Facturation mensuelle", "Multi-profil équipe"})
        );
    }
}
