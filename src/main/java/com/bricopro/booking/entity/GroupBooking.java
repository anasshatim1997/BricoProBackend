package com.bricopro.booking.entity;

import com.bricopro.user.entity.User;
import com.bricopro.user.entity.WorkerProfile.ServiceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "group_bookings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GroupBooking {

    public enum GroupBookingStatus { OPEN, PARTIAL, CONFIRMED, COMPLETED, CANCELLED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    @Enumerated(EnumType.STRING)
    private ServiceType serviceType;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String address;
    private LocalDate scheduledDate;
    private LocalTime scheduledStart;
    private int workersNeeded;
    private int workersConfirmed;
    private BigDecimal budgetPerWorker;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private GroupBookingStatus status = GroupBookingStatus.OPEN;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
