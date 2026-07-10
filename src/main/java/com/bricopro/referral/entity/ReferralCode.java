package com.bricopro.referral.entity;

import com.bricopro.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "referral_codes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReferralCode {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    private int timesUsed;
    private BigDecimal totalRewardsEarned;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
