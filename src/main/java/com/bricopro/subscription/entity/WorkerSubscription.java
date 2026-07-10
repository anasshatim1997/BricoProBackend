package com.bricopro.subscription.entity;

import com.bricopro.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "worker_subscriptions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkerSubscription {

    public enum Plan { FREE, PREMIUM, ENTERPRISE }
    public enum SubStatus { ACTIVE, EXPIRED, CANCELLED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private User worker;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Plan plan = Plan.FREE;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SubStatus subStatus = SubStatus.ACTIVE;

    private LocalDateTime startedAt;
    private LocalDateTime expiresAt;

    private BigDecimal amountPaid;
    private String paymentReference;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
