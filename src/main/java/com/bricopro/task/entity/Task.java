package com.bricopro.task.entity;

import com.bricopro.user.entity.User;
import com.bricopro.user.entity.WorkerProfile.ServiceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

@Entity
@Table(name = "tasks")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Schema(description = "A service request posted by a client and fulfilled by a worker")
public class Task {

    public enum TaskStatus {
        PENDING, SEARCHING, CONFIRMED, STARTED, COMPLETED, CANCELLED, DISPUTED
    }

    public enum CancelledBy { CLIENT, WORKER, ADMIN }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id")
    private User worker;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceType serviceType;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(columnDefinition = "DECIMAL(10,7)")
    private Double latitude;

    @Column(columnDefinition = "DECIMAL(10,7)")
    private Double longitude;

    @Column(nullable = false)
    private LocalDate scheduledDate;

    @Column(nullable = false)
    private LocalTime scheduledStart;

    private LocalTime scheduledEnd;

    private BigDecimal budgetMin;
    private BigDecimal budgetMax;
    private BigDecimal agreedPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TaskStatus status = TaskStatus.PENDING;

    @Column(nullable = false)
    @Builder.Default
    private boolean isUrgent = false;

    @Enumerated(EnumType.STRING)
    private CancelledBy cancelledBy;

    @Column(columnDefinition = "TEXT")
    private String cancellationReason;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL)
    private List<TaskPhoto> photos;

    @OneToMany(mappedBy = "task")
    private List<Review> reviews;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "bidding_enabled")
    private Boolean biddingEnabled;

    @Column(name = "bidding_deadline")
    private LocalDateTime biddingDeadline;

    @Column(name = "auto_assign_enabled")
    private Boolean autoAssignEnabled;
}