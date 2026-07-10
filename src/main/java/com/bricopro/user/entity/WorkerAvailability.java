package com.bricopro.user.entity;

import jakarta.persistence.*;
import com.bricopro.user.entity.WorkerProfile;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import io.swagger.v3.oas.annotations.media.Schema;

@Entity
@Table(name = "worker_availability",
        uniqueConstraints = @UniqueConstraint(columnNames = {"worker_id", "date"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Schema(description = "Daily availability slot declared by a worker")
public class WorkerAvailability {

    public enum AvailabilityStatus { AVAILABLE, BUSY, LEAVE }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private WorkerProfile workerProfile;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AvailabilityStatus status = AvailabilityStatus.AVAILABLE;

    private LocalTime startTime;
    private LocalTime endTime;
}
