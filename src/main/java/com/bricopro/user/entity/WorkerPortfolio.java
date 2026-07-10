package com.bricopro.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

@Entity
@Table(name = "worker_portfolio")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Schema(description = "Portfolio photo uploaded by a worker to showcase completed work")
public class WorkerPortfolio {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private WorkerProfile workerProfile;

    @Column(nullable = false, length = 500)
    private String photoUrl;

    private String caption;

    @Enumerated(EnumType.STRING)
    private WorkerProfile.ServiceType serviceType;

    @Column(nullable = false)
    @Builder.Default
    private int photoOrder = 0;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
