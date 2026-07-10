package com.bricopro.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import io.swagger.v3.oas.annotations.media.Schema;

@Entity
@Table(name = "worker_services",
        uniqueConstraints = @UniqueConstraint(columnNames = {"worker_id", "service_type"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Schema(description = "A service type offered by a worker together with the hourly rate")
public class WorkerService {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private WorkerProfile workerProfile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkerProfile.ServiceType serviceType;

    private BigDecimal hourlyRate;
}
