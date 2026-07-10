package com.bricopro.user.entity;

import jakarta.persistence.*;
import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;

@Entity
@Table(name = "client_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Schema(description = "Extended profile for CLIENT users — default address and preferences")
public class ClientProfile {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String companyName;
    private String city;

    @Column(columnDefinition = "TEXT")
    private String defaultAddress;

    @Column(columnDefinition = "DECIMAL(10,7)")
    private Double defaultLatitude;

    @Column(columnDefinition = "DECIMAL(10,7)")
    private Double defaultLongitude;
}