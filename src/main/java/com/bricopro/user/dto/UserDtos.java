package com.bricopro.user.dto;

import com.bricopro.user.entity.User.Role;
import com.bricopro.user.entity.User.Status;
import com.bricopro.user.entity.WorkerAvailability.AvailabilityStatus;
import com.bricopro.user.entity.WorkerProfile.ServiceType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class UserDtos {

    @Data
    public static class UserSummary {
        private Long id;
        private String firstName;
        private String lastName;
        private String avatarUrl;
        private Role role;
        private Status status;
        private boolean isVerified;
        private boolean isOnline;
        private LocalDateTime createdAt;
        private int cancellationCountThisMonth;
        private int reliabilityScore;
    }

    @Data
    public static class WorkerProfileResponse {
        private Long id;
        private UserSummary user;
        private String bio;
        private boolean cinVerified;
        private BigDecimal averageRating;
        private int totalReviews;
        private int totalMissions;
        private int interventionRadiusKm;
        private String city;
        private Double latitude;
        private Double longitude;
        private boolean isPremium;
        private List<WorkerServiceDto> services;
    }

    @Data
    public static class WorkerServiceDto {
        private ServiceType serviceType;
        private BigDecimal hourlyRate;
    }

    @Data
    public static class UpdateWorkerProfileRequest {
        private String bio;
        private Integer interventionRadiusKm;
        private String city;
        private Double latitude;
        private Double longitude;
        private String bankAccount;
        private List<WorkerServiceDto> services;
    }

    @Data
    public static class ClientProfileResponse {
        private Long id;
        private UserSummary user;
        private String companyName;
        private String city;
        private String defaultAddress;
        private Double defaultLatitude;
        private Double defaultLongitude;
    }

    @Data
    public static class UpdateClientProfileRequest {
        private String companyName;
        private String city;
        private String defaultAddress;
        private Double defaultLatitude;
        private Double defaultLongitude;
    }

    @Data
    public static class AvailabilityRequest {
        @NotNull
        private LocalDate date;
        @NotNull
        private AvailabilityStatus status;
        private LocalTime startTime;
        private LocalTime endTime;
    }

    @Data
    public static class AvailabilityResponse {
        private Long id;
        private LocalDate date;
        private AvailabilityStatus status;
        private LocalTime startTime;
        private LocalTime endTime;
    }

    @Data
    public static class UpdateUserRequest {
        @Size(min = 2, max = 100)
        private String firstName;
        @Size(min = 2, max = 100)
        private String lastName;
        private String avatarUrl;
    }
}