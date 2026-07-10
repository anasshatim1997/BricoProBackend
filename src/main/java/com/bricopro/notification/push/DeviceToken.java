package com.bricopro.notification.push;

import com.bricopro.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

// ─────────────────────────────────────────────────────────────────────────────
// Entity
// ─────────────────────────────────────────────────────────────────────────────
@Entity
@Table(name = "device_tokens",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "device_token"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class DeviceToken {
    public enum Platform {ANDROID, IOS}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 500)
    private String deviceToken;

    @Enumerated(EnumType.STRING)
    private Platform platform;

    @CreationTimestamp
    private LocalDateTime registeredAt;
}
