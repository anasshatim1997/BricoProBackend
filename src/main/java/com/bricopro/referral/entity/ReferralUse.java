package com.bricopro.referral.entity;

import com.bricopro.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "referral_uses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReferralUse {

    public enum RewardStatus { PENDING, CREDITED, EXPIRED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referrer_id", nullable = false)
    private User referrer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referred_id", nullable = false)
    private User referred;

    private String code;
    private BigDecimal referrerReward;
    private BigDecimal referredReward;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private RewardStatus rewardStatus = RewardStatus.PENDING;

    @CreationTimestamp
    private LocalDateTime usedAt;
    private LocalDateTime creditedAt;
}
