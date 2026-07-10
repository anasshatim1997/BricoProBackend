package com.bricopro.subscription.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sponsored_clicks", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"sponsored_worker_id", "viewer_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SponsoredClick {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sponsored_worker_id", nullable = false)
    private Long sponsoredWorkerId;

    @Column(name = "viewer_id", nullable = false)
    private Long viewerId;

    @CreationTimestamp
    private LocalDateTime clickedAt;
}
