package com.bricopro.geolocation;

import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;

@Data @Builder
public class NearbyWorkerDto {
    @Schema(description = "ID of the target user", example = "1")
    private Long userId;
    @Schema(description = "First name of the user", example = "example")
    private String firstName;
    @Schema(description = "Last name of the user", example = "example")
    private String lastName;
    @Schema(description = "URL pointing to the user profile picture", example = "example")
    private String avatarUrl;
    @Schema(description = "Average Rating", example = "value")
    private double averageRating;
    @Schema(description = "Total Reviews", example = "0")
    private int totalReviews;
    @Schema(description = "Distance Km", example = "value")
    private double distanceKm;
    @Schema(description = "City where the service is delivered", example = "example")
    private String city;
    @Schema(description = "Is Premium", example = "false")
    private boolean isPremium;
    @Schema(description = "Cin Verified", example = "false")
    private boolean cinVerified;
}
