package com.bricopro.trust;

import com.bricopro.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "worker_recommendations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"recommender_id", "worker_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkerRecommendation {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommender_id", nullable = false)
    private User recommender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private User worker;

    private String note;

    @CreationTimestamp
    private LocalDateTime createdAt;
}