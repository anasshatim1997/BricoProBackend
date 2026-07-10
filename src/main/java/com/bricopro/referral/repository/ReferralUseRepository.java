package com.bricopro.referral.repository;

import com.bricopro.referral.entity.ReferralUse;
import com.bricopro.referral.entity.ReferralUse.RewardStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReferralUseRepository extends JpaRepository<ReferralUse, Long> {
    boolean existsByReferredId(Long referredId);
    List<ReferralUse> findByReferrerId(Long referrerId);
    Optional<ReferralUse> findByReferredIdAndRewardStatus(Long referredId, RewardStatus status);
}
