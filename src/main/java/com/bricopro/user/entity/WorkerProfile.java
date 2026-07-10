package com.bricopro.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "worker_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkerProfile {

    public enum ServiceType {
        REPAIRS, ASSEMBLY, MOVING, CLEANING, PAINTING, CONSTRUCTION, OUTDOOR, DECORATION, PLUMBING
    }

    public enum VerificationStatus {
        UNSUBMITTED, PENDING, VERIFIED, REJECTED
    }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String cinDocumentUrl;

    @Column(nullable = false)
    @Builder.Default
    private boolean cinVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.UNSUBMITTED;

    @Column(unique = true, length = 12)
    private String cinNumber;

    @Column(columnDefinition = "TEXT")
    private String cinRejectionReason;

    private LocalDateTime cinSubmittedAt;

    @Column(nullable = false, precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private int totalReviews = 0;

    @Column(nullable = false)
    @Builder.Default
    private int totalMissions = 0;

    @Column(nullable = false)
    @Builder.Default
    private int interventionRadiusKm = 20;

    private String city;

    @Column(columnDefinition = "DECIMAL(10,7)")
    private Double latitude;

    @Column(columnDefinition = "DECIMAL(10,7)")
    private Double longitude;

    private String bankAccount;

    @Column(nullable = false)
    @Builder.Default
    private boolean isPremium = false;

    @Column(nullable = false)
    @Builder.Default
    private int cancellationCount = 0;

    @Column(nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal responseRate = new BigDecimal("100.00");

    @Builder.Default
    private int cancellationCountThisMonth = 0;

    @Builder.Default
    private int totalCancellationsLifetime = 0;

    @Builder.Default
    private int reliabilityScore = 100;

    private LocalDateTime visibilityReductionUntil;

    @Builder.Default
    private boolean verifiedBadge = true;

    @OneToMany(mappedBy = "workerProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<WorkerService> services;
}