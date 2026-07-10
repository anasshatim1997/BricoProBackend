package com.bricopro.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "worker_snapshot_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkerSnapshotHistory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "worker_id", nullable = false)
    private Long workerId;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "average_rating", nullable = false)
    private BigDecimal averageRating;

    @Column(name = "total_reviews", nullable = false)
    private int totalReviews;

    @Column(name = "total_missions", nullable = false)
    private int totalMissions;

    @Column(name = "response_rate", nullable = false)
    private BigDecimal responseRate;

    @Column(name = "cancellation_count", nullable = false)
    private int cancellationCount;

    @Column(name = "is_premium", nullable = false)
    private boolean isPremium;

    @CreationTimestamp
    @Column(name = "snapshotted_at")
    private LocalDateTime snapshottedAt;
}
