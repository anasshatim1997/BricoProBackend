package com.bricopro.badge.service;

import com.bricopro.badge.entity.WorkerBadge;
import com.bricopro.badge.repository.WorkerBadgeRepository;
import com.bricopro.task.repository.ReviewRepository;
import com.bricopro.task.repository.TaskRepository;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.WorkerProfile;
import com.bricopro.user.repository.WorkerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BadgeService {

    private final WorkerBadgeRepository   badgeRepository;
    private final WorkerProfileRepository workerProfileRepository;
    private final TaskRepository          taskRepository;
    private final ReviewRepository        reviewRepository;

    private static final Map<WorkerBadge.BadgeType, String[]> BADGE_INFO = Map.of(
        WorkerBadge.BadgeType.NEW_WORKER,          new String[]{"Nouveau",         "Bienvenue sur BricoPro !",               "🆕"},
        WorkerBadge.BadgeType.VERIFIED_CIN,        new String[]{"Identité vérifiée","CIN vérifié par BricoPro",              "✅"},
        WorkerBadge.BadgeType.TOP_RATED,           new String[]{"Très bien noté",  "Note moyenne ≥ 4.5 étoiles",            "⭐"},
        WorkerBadge.BadgeType.EXPERIENCED,         new String[]{"Expérimenté",     "Plus de 50 missions réalisées",          "🏆"},
        WorkerBadge.BadgeType.PREMIUM,             new String[]{"Premium",          "Abonné BricoPro Premium",               "💎"},
        WorkerBadge.BadgeType.FAST_RESPONDER,      new String[]{"Réactif",          "Taux de réponse ≥ 90%",                 "⚡"},
        WorkerBadge.BadgeType.ZERO_CANCELLATIONS,  new String[]{"Fiable",           "Aucune annulation",                     "🎯"},
        WorkerBadge.BadgeType.ELITE,               new String[]{"Elite",            "Top 5% des prestataires",               "🥇"}
    );

    @Transactional
    public List<WorkerBadge> evaluateAndAssign(Long userId) {
        WorkerProfile profile = workerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Worker not found"));

        List<WorkerBadge> newBadges = new ArrayList<>();

        awardIfEligible(userId, WorkerBadge.BadgeType.NEW_WORKER,
                profile.getTotalMissions() == 0, newBadges);

        awardIfEligible(userId, WorkerBadge.BadgeType.VERIFIED_CIN,
                profile.isCinVerified(), newBadges);

        awardIfEligible(userId, WorkerBadge.BadgeType.TOP_RATED,
                profile.getAverageRating().doubleValue() >= 4.5 && profile.getTotalReviews() >= 5, newBadges);

        awardIfEligible(userId, WorkerBadge.BadgeType.EXPERIENCED,
                profile.getTotalMissions() >= 50, newBadges);

        awardIfEligible(userId, WorkerBadge.BadgeType.PREMIUM,
                profile.isPremium(), newBadges);

        awardIfEligible(userId, WorkerBadge.BadgeType.FAST_RESPONDER,
                profile.getResponseRate().doubleValue() >= 90, newBadges);

        awardIfEligible(userId, WorkerBadge.BadgeType.ZERO_CANCELLATIONS,
                profile.getCancellationCount() == 0 && profile.getTotalMissions() >= 5, newBadges);

        awardIfEligible(userId, WorkerBadge.BadgeType.ELITE,
                profile.getAverageRating().doubleValue() >= 4.8
                        && profile.getTotalMissions() >= 100
                        && profile.isCinVerified()
                        && profile.getCancellationCount() == 0, newBadges);

        return newBadges;
    }

    public List<WorkerBadge> getBadges(Long userId) {
        return badgeRepository.findByUserId(userId);
    }

    private void awardIfEligible(Long userId, WorkerBadge.BadgeType type,
                                  boolean condition, List<WorkerBadge> awarded) {
        if (condition && !badgeRepository.existsByUserIdAndBadgeType(userId, type)) {
            String[] info = BADGE_INFO.get(type);
            User user = new User();
            user.setId(userId);
            WorkerBadge badge = WorkerBadge.builder()
                    .user(user).badgeType(type)
                    .label(info[0]).description(info[1]).iconUrl(info[2])
                    .build();
            awarded.add(badgeRepository.save(badge));
        }
    }
}
