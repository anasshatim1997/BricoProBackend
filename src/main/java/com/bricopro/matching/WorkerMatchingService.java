package com.bricopro.matching;

import com.bricopro.geolocation.GeolocationService;
import com.bricopro.user.entity.WorkerProfile;
import com.bricopro.user.entity.WorkerProfile.ServiceType;
import com.bricopro.user.repository.WorkerProfileRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Worker Matching Service", description = "Business logic for Worker Matching Service")
@Service
@RequiredArgsConstructor
public class WorkerMatchingService {

    @Schema(description = "Worker Profile Repository", example = "value")
    private final WorkerProfileRepository workerProfileRepository;

private static final double WEIGHT_DISTANCE      = 30.0;
    private static final double WEIGHT_RATING        = 25.0;
    private static final double WEIGHT_RESPONSE_RATE = 20.0;
    private static final double WEIGHT_MISSIONS      = 15.0;
    private static final double WEIGHT_PREMIUM       = 5.0;
    private static final double WEIGHT_CIN           = 5.0;

    /**
     * Find Best Matches.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public List<MatchedWorkerDto> findBestMatches(MatchRequest req) {
        List<WorkerProfile> candidates = workerProfileRepository
                .findNearbyWorkers(req.getLatitude(), req.getLongitude(),
                        req.getRadiusKm(), req.getServiceType(), PageRequest.of(0, 50))
                .getContent();

        return candidates.stream()
                .map(w -> score(w, req))
                .sorted(Comparator.comparingDouble(MatchedWorkerDto::getScore).reversed())
                .limit(req.getLimit())
                .collect(Collectors.toList());
    }

    private MatchedWorkerDto score(WorkerProfile w, MatchRequest req) {
        double distKm = GeolocationService.haversine(
                req.getLatitude(), req.getLongitude(),
                w.getLatitude() != null ? w.getLatitude() : 0,
                w.getLongitude() != null ? w.getLongitude() : 0);

double distScore = WEIGHT_DISTANCE * Math.max(0, 1.0 - (distKm / req.getRadiusKm()));

double ratingScore = WEIGHT_RATING * (w.getAverageRating().doubleValue() / 5.0);

double responseScore = WEIGHT_RESPONSE_RATE * (w.getResponseRate().doubleValue() / 100.0);

double missionScore = WEIGHT_MISSIONS * Math.min(1.0, Math.log10(w.getTotalMissions() + 1) / 2.0);

double premiumScore = w.isPremium() ? WEIGHT_PREMIUM : 0;

double cinScore = w.isCinVerified() ? WEIGHT_CIN : 0;

double cancellationPenalty = Math.min(20, w.getCancellationCount() * 2.0);

        double total = distScore + ratingScore + responseScore + missionScore
                     + premiumScore + cinScore - cancellationPenalty;

        return MatchedWorkerDto.builder()
                .userId(w.getUser().getId())
                .firstName(w.getUser().getFirstName())
                .lastName(w.getUser().getLastName())
                .avatarUrl(w.getUser().getAvatarUrl())
                .averageRating(w.getAverageRating().doubleValue())
                .totalReviews(w.getTotalReviews())
                .totalMissions(w.getTotalMissions())
                .distanceKm(Math.round(distKm * 10.0) / 10.0)
                .responseRate(w.getResponseRate().doubleValue())
                .isPremium(w.isPremium())
                .isCinVerified(w.isCinVerified())
                .score(Math.round(total * 10.0) / 10.0)
                .matchReason(buildMatchReason(distKm, w))
                .build();
    }

    private String buildMatchReason(double distKm, WorkerProfile w) {
        if (w.isPremium() && w.getAverageRating().doubleValue() >= 4.5)
            return "Top prestataire premium près de vous";
        if (distKm < 2)
            return "Très proche — " + (int) distKm + " km";
        if (w.getAverageRating().doubleValue() >= 4.5)
            return "Excellent avis clients";
        if (w.getTotalMissions() > 50)
            return "Très expérimenté — " + w.getTotalMissions() + " missions";
        return "Disponible près de vous";
    }

    @Data
    @Builder
    @Schema(description = "Request payload for: Match.")
    public static class MatchRequest {
        @Schema(description = "GPS latitude coordinate", example = "value")
        private double latitude;
        @Schema(description = "GPS longitude coordinate", example = "value")
        private double longitude;
        @Schema(description = "Maximum intervention radius in kilometres", example = "value")
        private double radiusKm;
        @Schema(description = "Service category: REPAIRS, PLUMBING, CLEANING, PAINTING, etc.", example = "value")
        private ServiceType serviceType;
        @Schema(description = "Limit", example = "0")
        private int limit;
    }

    @Data
    @Builder
    @Schema(description = "Data transfer object: Matched Worker.")
    public static class MatchedWorkerDto {
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
        @Schema(description = "Total Missions", example = "0")
        private int totalMissions;
        @Schema(description = "Distance Km", example = "value")
        private double distanceKm;
        @Schema(description = "Response Rate", example = "value")
        private double responseRate;
        @Schema(description = "Is Premium", example = "false")
        private boolean isPremium;
        @Schema(description = "Is Cin Verified", example = "false")
        private boolean isCinVerified;
        @Schema(description = "Score", example = "value")
        private double score;
        @Schema(description = "Match Reason", example = "example")
        private String matchReason;
    }
}
