package com.bricopro.preference.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_preferences")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserPreference {

    @Id
    private Long userId;

    @Column(nullable = false)
    @Builder.Default
    private String language = "fr";

    @Column(nullable = false)
    @Builder.Default
    private String theme = "light";

    @Column(nullable = false)
    @Builder.Default
    private boolean pushEnabled = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean emailEnabled = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean smsEnabled = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean marketingEnabled = false;

    private String defaultCity;

    @Column(columnDefinition = "DECIMAL(10,7)")
    private Double defaultLatitude;

    @Column(columnDefinition = "DECIMAL(10,7)")
    private Double defaultLongitude;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
