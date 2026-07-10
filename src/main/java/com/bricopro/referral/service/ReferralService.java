package com.bricopro.referral.service;

import com.bricopro.notification.service.CommunicationService;
import com.bricopro.referral.entity.ReferralCode;
import com.bricopro.referral.entity.ReferralUse;
import com.bricopro.referral.entity.ReferralUse.RewardStatus;
import com.bricopro.referral.repository.ReferralCodeRepository;
import com.bricopro.referral.repository.ReferralUseRepository;
import com.bricopro.user.entity.User;
import com.bricopro.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReferralService {

    private static final BigDecimal REFERRER_REWARD = new BigDecimal("50");
    private static final BigDecimal REFERRED_REWARD = new BigDecimal("30");

    private final ReferralCodeRepository referralCodeRepository;
    private final ReferralUseRepository  referralUseRepository;
    private final UserRepository         userRepository;
    private final CommunicationService   communicationService;

    public ReferralCode getOrCreateCode(Long userId) {
        return referralCodeRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            String code = "BRICO" + userId + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
            return referralCodeRepository.save(ReferralCode.builder()
                    .user(user).code(code)
                    .timesUsed(0)
                    .totalRewardsEarned(BigDecimal.ZERO)
                    .build());
        });
    }

    @Transactional
    public Map<String, Object> applyReferralCode(Long newUserId, String code) {
        if (referralUseRepository.existsByReferredId(newUserId))
            throw new IllegalStateException("Referral code already applied to your account");

        ReferralCode referralCode = referralCodeRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Invalid referral code"));

        if (referralCode.getUser().getId().equals(newUserId))
            throw new IllegalArgumentException("Cannot use your own referral code");

        User referrer = referralCode.getUser();
        User referred = userRepository.findById(newUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        ReferralUse use = ReferralUse.builder()
                .referrer(referrer).referred(referred).code(code)
                .referrerReward(REFERRER_REWARD).referredReward(REFERRED_REWARD)
                .rewardStatus(RewardStatus.PENDING)
                .build();
        referralUseRepository.save(use);

        referralCode.setTimesUsed(referralCode.getTimesUsed() + 1);
        referralCodeRepository.save(referralCode);

        if (referrer.getEmail() != null) {
            communicationService.sendEmail(referrer.getEmail(),
                    "BricoPro — Parrainage réussi !",
                    referred.getFirstName() + " a rejoint BricoPro grâce à vous ! " +
                    "Vous recevrez " + REFERRER_REWARD + " MAD de crédit après sa première mission.");
        }

        return Map.of(
                "message",         "Code appliqué avec succès !",
                "referredReward",  REFERRED_REWARD + " MAD offerts sur votre première mission",
                "referrerName",    referrer.getFirstName()
        );
    }

    @Transactional
    public void creditPendingRewardIfFirstCompletedTask(Long referredUserId) {
        referralUseRepository.findByReferredIdAndRewardStatus(referredUserId, RewardStatus.PENDING)
                .ifPresent(use -> {
                    use.setRewardStatus(RewardStatus.CREDITED);
                    use.setCreditedAt(LocalDateTime.now());
                    referralUseRepository.save(use);

                    ReferralCode referrerCode = referralCodeRepository.findByUserId(use.getReferrer().getId())
                            .orElse(null);
                    if (referrerCode != null) {
                        referrerCode.setTotalRewardsEarned(
                                referrerCode.getTotalRewardsEarned().add(use.getReferrerReward()));
                        referralCodeRepository.save(referrerCode);
                    }

                    log.info("Referral reward credited: referrer={} referred={} amount={}",
                            use.getReferrer().getId(), referredUserId, use.getReferrerReward());

                    if (use.getReferrer().getEmail() != null) {
                        communicationService.sendEmail(use.getReferrer().getEmail(),
                                "BricoPro — Récompense de parrainage créditée !",
                                use.getReferred().getFirstName() + " a terminé sa première mission. " +
                                use.getReferrerReward() + " MAD ont été ajoutés à votre solde de récompenses.");
                    }
                });
    }

    public Map<String, Object> getStats(Long userId) {
        List<ReferralUse> uses = referralUseRepository.findByReferrerId(userId);
        BigDecimal totalEarned = referralCodeRepository.findByUserId(userId)
                .map(ReferralCode::getTotalRewardsEarned).orElse(BigDecimal.ZERO);

        return Map.of(
                "code",          getOrCreateCode(userId).getCode(),
                "timesUsed",     uses.size(),
                "totalEarned",   totalEarned,
                "pendingRewards", uses.stream()
                        .filter(u -> u.getRewardStatus() == RewardStatus.PENDING)
                        .count()
        );
    }
}
