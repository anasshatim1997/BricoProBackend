package com.bricopro.notification.entity;

import com.bricopro.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Schema(description = "In-app notification delivered to a user about a platform event")
public class Notification {

    public enum NotificationType {
        NEW_TASK, TASK_ACCEPTED, TASK_STARTED, TASK_COMPLETED,
        TASK_CANCELLED, NEW_MESSAGE, PAYMENT_RECEIVED,
        PAYMENT_AWAITING_CONFIRMATION, PAYMENT_DISPUTED,
        REVIEW_RECEIVED, ACCOUNT_VERIFIED, SYSTEM
    }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    private Long referenceId;
    private String referenceType;

    @Column(nullable = false)
    @Builder.Default
    private boolean isRead = false;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
