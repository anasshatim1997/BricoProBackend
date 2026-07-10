package com.bricopro.task.service;

import com.bricopro.notification.service.CommunicationService;
import com.bricopro.task.repository.ReviewRepository;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.User.Status;
import com.bricopro.user.entity.WorkerProfile;
import com.bricopro.user.repository.UserRepository;
import com.bricopro.user.repository.WorkerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Rating Suspension Service", description = "Business logic for Rating Suspension Service")
@Service
@RequiredArgsConstructor
@Slf4j
public class RatingSuspensionService {

    private static final double MIN_RATING_THRESHOLD = 3.0;
    private static final int    MIN_REVIEWS_REQUIRED = 10;

    private final WorkerProfileRepository workerProfileRepository;
    private final UserRepository          userRepository;
    private final ReviewRepository        reviewRepository;
    private final CommunicationService    communicationService;

@Transactional
    /**
     * Evaluate Worker Rating.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public void evaluateWorkerRating(Long workerId) {
        WorkerProfile profile = workerProfileRepository.findByUserId(workerId)
                .orElseThrow(() -> new IllegalArgumentException("Worker profile not found"));

if (profile.getTotalReviews() < MIN_REVIEWS_REQUIRED) {
            return;
        }

        Double avg = reviewRepository.calculateAverageRating(workerId);
        if (avg == null) return;

        if (avg < MIN_RATING_THRESHOLD) {
            User worker = profile.getUser();

            if (worker.getStatus() == Status.ACTIVE) {
                worker.setStatus(Status.SUSPENDED);
                userRepository.save(worker);

                log.warn("Worker {} suspended — rating {:.2f} < {} on {} reviews",
                        workerId, avg, MIN_RATING_THRESHOLD, profile.getTotalReviews());

                communicationService.sendEmail(
                        worker.getEmail(),
                        "BricoPro — Compte suspendu (note insuffisante)",
                        "Bonjour " + worker.getFirstName() + ",\n\n" +
                                "Votre compte a été suspendu car votre note moyenne (" +
                                String.format("%.2f", avg) + "/5) est inférieure au seuil minimum " +
                                "de " + MIN_RATING_THRESHOLD + "/5 sur " + profile.getTotalReviews() + " avis.\n\n" +
                                "Pour contester cette décision ou améliorer votre profil, " +
                                "contactez notre équipe à support@bricopro.ma.\n\n" +
                                "L'équipe BricoPro");

                if (worker.getPhone() != null) {
                    communicationService.sendWhatsApp(
                            worker.getPhone(),
                            "BricoPro: Votre compte a été suspendu (note < 3.0). " +
                                    "Contactez support@bricopro.ma");
                }
            }
        }
    }
}

