package com.bricopro.booking.entity;

import com.bricopro.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "group_booking_workers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GroupBookingWorker {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_booking_id", nullable = false)
    private GroupBooking groupBooking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private User worker;

    private Long taskId;

    @CreationTimestamp
    private LocalDateTime joinedAt;
}
