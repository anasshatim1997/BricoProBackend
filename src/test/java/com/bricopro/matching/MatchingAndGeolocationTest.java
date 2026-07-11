package com.bricopro.matching;

import com.bricopro.geolocation.GeolocationService;
import com.bricopro.geolocation.NearbyWorkerDto;
import com.bricopro.matching.WorkerMatchingService.MatchRequest;
import com.bricopro.matching.WorkerMatchingService.MatchedWorkerDto;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.User.Role;
import com.bricopro.user.entity.WorkerProfile;
import com.bricopro.user.entity.WorkerProfile.ServiceType;
import com.bricopro.user.repository.WorkerProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Matching & Geolocation")
class MatchingAndGeolocationTest {

    // ─── GEOLOCATION SERVICE ──────────────────────────────────────────────────

    @Nested
    @DisplayName("GeolocationService")
    class GeolocationServiceTests {

        @Test
        @DisplayName("haversine distance between identical points is 0")
        void samePointDistance() {
            double dist = GeolocationService.haversine(33.589886, -7.603869, 33.589886, -7.603869);
            assertThat(dist).isEqualTo(0.0);
        }

        @Test
        @DisplayName("haversine distance Casablanca → Rabat ≈ 87 km")
        void casablancaToRabat() {
            // Casablanca: 33.5731, -7.5898  |  Rabat: 34.020882, -6.84165
            double dist = GeolocationService.haversine(33.5731, -7.5898, 34.020882, -6.84165);
            assertThat(dist).isBetween(85.0, 92.0);
        }

        @Test
        @DisplayName("haversine distance is symmetric")
        void symmetricDistance() {
            double d1 = GeolocationService.haversine(33.5731, -7.5898, 34.020882, -6.84165);
            double d2 = GeolocationService.haversine(34.020882, -6.84165, 33.5731, -7.5898);
            assertThat(d1).isCloseTo(d2, within(0.001));
        }

        @Test
        @DisplayName("haversine distance Casablanca → Marrakech ≈ 219 km")
        void casablancaToMarrakech() {
            double dist = GeolocationService.haversine(33.5731, -7.5898, 31.6295, -7.9811);
            assertThat(dist).isBetween(215.0, 225.0);
        }

        @Test
        @DisplayName("very short distance is non-negative")
        void shortDistanceNonNegative() {
            double dist = GeolocationService.haversine(33.589886, -7.603869, 33.590000, -7.604000);
            assertThat(dist).isGreaterThanOrEqualTo(0.0);
        }
    }

    // ─── WORKER MATCHING SERVICE ──────────────────────────────────────────────

    @Nested
    @DisplayName("WorkerMatchingService")
    class WorkerMatchingServiceTests {

        @Mock WorkerProfileRepository workerProfileRepository;
        @InjectMocks WorkerMatchingService matchingService;

        private WorkerProfile premiumHighRated;
        private WorkerProfile basicLowRated;
        private User premiumUser;
        private User basicUser;

        @BeforeEach
        void setup() {
            premiumUser = User.builder().id(1L).firstName("Elite").lastName("Worker")
                    .avatarUrl("http://img/1.jpg").role(Role.WORKER).build();

            basicUser = User.builder().id(2L).firstName("Basic").lastName("Worker")
                    .avatarUrl("http://img/2.jpg").role(Role.WORKER).build();

            premiumHighRated = WorkerProfile.builder()
                    .id(1L).user(premiumUser)
                    .latitude(33.589886).longitude(-7.603869)  // 0 km from request
                    .averageRating(BigDecimal.valueOf(4.8))
                    .totalReviews(50).totalMissions(100)
                    .responseRate(BigDecimal.valueOf(95))
                    .isPremium(true).cinVerified(true)
                    .cancellationCount(0)
                    .build();

            basicLowRated = WorkerProfile.builder()
                    .id(2L).user(basicUser)
                    .latitude(33.600000).longitude(-7.610000)  // ~1.5 km away
                    .averageRating(BigDecimal.valueOf(3.5))
                    .totalReviews(5).totalMissions(10)
                    .responseRate(BigDecimal.valueOf(60))
                    .isPremium(false).cinVerified(false)
                    .cancellationCount(3)
                    .build();
        }

        @Test
        @DisplayName("returns workers sorted by score descending")
        void sortedByScoreDesc() {
            MatchRequest req = MatchRequest.builder()
                    .latitude(33.589886).longitude(-7.603869)
                    .radiusKm(25).serviceType(ServiceType.PLUMBING).limit(10)
                    .build();

            Page<WorkerProfile> page = new PageImpl<>(List.of(premiumHighRated, basicLowRated));
            when(workerProfileRepository.findNearbyWorkers(
                    anyDouble(), anyDouble(), anyDouble(), any(), any())).thenReturn(page);

            List<MatchedWorkerDto> results = matchingService.findBestMatches(req);

            assertThat(results).isNotEmpty();
            assertThat(results.get(0).getScore()).isGreaterThanOrEqualTo(results.get(results.size() - 1).getScore());
        }

        @Test
        @DisplayName("premium + high rated worker ranks above basic + low rated")
        void premiumRanksHigher() {
            MatchRequest req = MatchRequest.builder()
                    .latitude(33.589886).longitude(-7.603869)
                    .radiusKm(25).limit(10)
                    .build();

            Page<WorkerProfile> page = new PageImpl<>(List.of(premiumHighRated, basicLowRated));
            when(workerProfileRepository.findNearbyWorkers(
                    anyDouble(), anyDouble(), anyDouble(), any(), any())).thenReturn(page);

            List<MatchedWorkerDto> results = matchingService.findBestMatches(req);

            // Premium, high-rated, 0-distance worker should outrank basic
            assertThat(results.get(0).getUserId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("respects the limit parameter")
        void respectsLimit() {
            MatchRequest req = MatchRequest.builder()
                    .latitude(33.589886).longitude(-7.603869)
                    .radiusKm(25).limit(1)
                    .build();

            Page<WorkerProfile> page = new PageImpl<>(List.of(premiumHighRated, basicLowRated));
            when(workerProfileRepository.findNearbyWorkers(
                    anyDouble(), anyDouble(), anyDouble(), any(), any())).thenReturn(page);

            List<MatchedWorkerDto> results = matchingService.findBestMatches(req);
            assertThat(results).hasSize(1);
        }

        @Test
        @DisplayName("all results have non-null matchReason")
        void matchReasonNeverNull() {
            MatchRequest req = MatchRequest.builder()
                    .latitude(33.589886).longitude(-7.603869)
                    .radiusKm(25).limit(10)
                    .build();

            Page<WorkerProfile> page = new PageImpl<>(List.of(premiumHighRated, basicLowRated));
            when(workerProfileRepository.findNearbyWorkers(
                    anyDouble(), anyDouble(), anyDouble(), any(), any())).thenReturn(page);

            List<MatchedWorkerDto> results = matchingService.findBestMatches(req);
            results.forEach(r -> assertThat(r.getMatchReason()).isNotBlank());
        }

        @Test
        @DisplayName("worker with many cancellations has lower score than identical worker with none")
        void cancellationsPenalty() {
            // Same as basicLowRated but with 0 cancellations
            WorkerProfile noCancellations = WorkerProfile.builder()
                    .id(3L).user(basicUser)
                    .latitude(33.600000).longitude(-7.610000)
                    .averageRating(BigDecimal.valueOf(3.5))
                    .totalReviews(5).totalMissions(10)
                    .responseRate(BigDecimal.valueOf(60))
                    .isPremium(false).cinVerified(false)
                    .cancellationCount(0) // key difference
                    .build();

            MatchRequest req = MatchRequest.builder()
                    .latitude(33.589886).longitude(-7.603869)
                    .radiusKm(25).limit(10)
                    .build();

            Page<WorkerProfile> page1 = new PageImpl<>(List.of(basicLowRated));
            Page<WorkerProfile> page2 = new PageImpl<>(List.of(noCancellations));

            when(workerProfileRepository.findNearbyWorkers(
                    anyDouble(), anyDouble(), anyDouble(), any(), any()))
                    .thenReturn(page1)
                    .thenReturn(page2);

            List<MatchedWorkerDto> withCancellations = matchingService.findBestMatches(req);
            List<MatchedWorkerDto> without = matchingService.findBestMatches(req);

            assertThat(without.get(0).getScore()).isGreaterThan(withCancellations.get(0).getScore());
        }

        @Test
        @DisplayName("returns empty list when no nearby workers")
        void emptyResult() {
            MatchRequest req = MatchRequest.builder()
                    .latitude(33.589886).longitude(-7.603869)
                    .radiusKm(1).limit(10)
                    .build();

            when(workerProfileRepository.findNearbyWorkers(
                    anyDouble(), anyDouble(), anyDouble(), any(), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            List<MatchedWorkerDto> results = matchingService.findBestMatches(req);
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("distance is correctly computed and stored in DTO")
        void distanceInDto() {
            MatchRequest req = MatchRequest.builder()
                    .latitude(33.589886).longitude(-7.603869)
                    .radiusKm(25).limit(10)
                    .build();

            Page<WorkerProfile> page = new PageImpl<>(List.of(premiumHighRated));
            when(workerProfileRepository.findNearbyWorkers(
                    anyDouble(), anyDouble(), anyDouble(), any(), any())).thenReturn(page);

            List<MatchedWorkerDto> results = matchingService.findBestMatches(req);
            assertThat(results.get(0).getDistanceKm()).isEqualTo(0.0); // same coordinates
        }
    }
}
