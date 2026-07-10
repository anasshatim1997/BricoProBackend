package com.bricopro.tracking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "worker_locations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkerLocation {

    @Id
    private Long workerId;

    @Column(columnDefinition = "DECIMAL(10,7)")
    private Double latitude;

    @Column(columnDefinition = "DECIMAL(10,7)")
    private Double longitude;

    private Double speedKmh;
    private Double headingDegrees;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
