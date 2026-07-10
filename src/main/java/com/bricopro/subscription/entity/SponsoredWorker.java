package com.bricopro.subscription.entity;

import com.bricopro.user.entity.User;
import com.bricopro.user.entity.WorkerProfile.ServiceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sponsored_workers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SponsoredWorker {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private User worker;

    @Enumerated(EnumType.STRING)
    private ServiceType serviceType;

    private String targetCity;
    private BigDecimal dailyBudget;
    private BigDecimal spent;
    private BigDecimal costPerClick;
    private long impressions;
    private long clicks;

    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private boolean active;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
