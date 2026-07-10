package com.bricopro.badge.entity;

import com.bricopro.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "worker_badges")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkerBadge {

    public enum BadgeType {
        NEW_WORKER, VERIFIED_CIN, TOP_RATED, EXPERIENCED,
        PREMIUM, FAST_RESPONDER, ZERO_CANCELLATIONS, ELITE
    }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BadgeType badgeType;

    private String label;
    private String description;
    private String iconUrl;

    @CreationTimestamp
    private LocalDateTime earnedAt;
}
