package com.bricopro.verification.dto;

import com.bricopro.user.entity.WorkerProfile.VerificationStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

public class CinVerificationDtos {

    @Data
    @Builder
    public static class WorkerProfileDetailResponse {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private String bio;
        private String city;
        private Integer interventionRadiusKm;
        private Double latitude;
        private Double longitude;
        private String bankAccount;
        private boolean verified;
        private VerificationStatus verificationStatus;
        private List<WorkerServiceDto> services;
        private BigDecimal rating;
        private Integer completedMissions;
        private Integer reviewsCount;
        private String cinImageUrl;
        private String cinRejectionReason;
    }

    @Data
    @Builder
    public static class WorkerServiceDto {
        private String serviceType;
        private BigDecimal hourlyRate;
    }

    @Data
    @Builder
    public static class CinVerificationResponse {
        private VerificationStatus verificationStatus;
        private String cinNumber;
        private String cinImageUrl;
        private String message;
    }
}