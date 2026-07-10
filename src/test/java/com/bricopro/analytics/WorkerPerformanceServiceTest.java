package com.bricopro.analytics;

import com.bricopro.analytics.service.WorkerPerformanceReportCacheService;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.WorkerProfile;
import com.bricopro.user.repository.WorkerProfileRepository;
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
@DisplayName("WorkerPerformanceService")
class WorkerPerformanceServiceTest {

    @Mock WorkerProfileRepository workerProfileRepository;
    @Mock WorkerPerformanceReportCacheService reportCacheService;

    @InjectMocks WorkerPerformanceService performanceService;

    @Nested
    @DisplayName("getReport()")
    class GetReport {

        @Test
        @DisplayName("delegates to the separate cached bean, not to itself")
        void delegatesToCachedBean() {
            WorkerPerformanceService.PerformanceReport report =
                    WorkerPerformanceService.PerformanceReport.builder().workerId(5L).build();
            when(reportCacheService.getReport(5L)).thenReturn(report);

            WorkerPerformanceService.PerformanceReport result = performanceService.getReport(5L);

            assertThat(result.getWorkerId()).isEqualTo(5L);
            verify(reportCacheService).getReport(5L);
        }
    }

    @Nested
    @DisplayName("getLeaderboard() — the fixed caching path")
    class GetLeaderboard {

        @Test
        @DisplayName("REGRESSION: calls the separate cached bean for every candidate, not an internal self-invocation")
        void callsSeparateCachedBeanForEachCandidate() {
            User worker1 = User.builder().id(1L).build();
            User worker2 = User.builder().id(2L).build();

            WorkerProfile wp1 = WorkerProfile.builder()
                    .user(worker1).averageRating(BigDecimal.valueOf(4.8))
                    .totalMissions(80).responseRate(BigDecimal.valueOf(95))
                    .cancellationCount(0).totalReviews(40).build();
            WorkerProfile wp2 = WorkerProfile.builder()
                    .user(worker2).averageRating(BigDecimal.valueOf(3.5))
                    .totalMissions(10).responseRate(BigDecimal.valueOf(60))
                    .cancellationCount(2).totalReviews(5).build();

            Page<WorkerProfile> page = new PageImpl<>(List.of(wp1, wp2));
            when(workerProfileRepository.findAll(any(Pageable.class))).thenReturn(page);

            WorkerPerformanceService.PerformanceReport report1 =
                    WorkerPerformanceService.PerformanceReport.builder()
                            .workerId(1L).performanceScore(90).build();
            WorkerPerformanceService.PerformanceReport report2 =
                    WorkerPerformanceService.PerformanceReport.builder()
                            .workerId(2L).performanceScore(40).build();

            when(reportCacheService.getReport(1L)).thenReturn(report1);
            when(reportCacheService.getReport(2L)).thenReturn(report2);

            List<WorkerPerformanceService.PerformanceReport> result = performanceService.getLeaderboard(10);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getWorkerId()).isEqualTo(1L);
            verify(reportCacheService).getReport(1L);
            verify(reportCacheService).getReport(2L);
        }

        @Test
        @DisplayName("ranks the higher-scored worker first")
        void ranksHigherScoreFirst() {
            User worker1 = User.builder().id(1L).build();
            User worker2 = User.builder().id(2L).build();

            WorkerProfile lowScore = WorkerProfile.builder()
                    .user(worker1).averageRating(BigDecimal.valueOf(3.0))
                    .totalMissions(5).responseRate(BigDecimal.valueOf(50))
                    .cancellationCount(3).totalReviews(2).build();
            WorkerProfile highScore = WorkerProfile.builder()
                    .user(worker2).averageRating(BigDecimal.valueOf(5.0))
                    .totalMissions(200).responseRate(BigDecimal.valueOf(99))
                    .cancellationCount(0).totalReviews(150).build();

            when(workerProfileRepository.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(lowScore, highScore)));

            when(reportCacheService.getReport(1L)).thenReturn(
                    WorkerPerformanceService.PerformanceReport.builder().workerId(1L).performanceScore(20).build());
            when(reportCacheService.getReport(2L)).thenReturn(
                    WorkerPerformanceService.PerformanceReport.builder().workerId(2L).performanceScore(95).build());

            List<WorkerPerformanceService.PerformanceReport> result = performanceService.getLeaderboard(10);

            assertThat(result.get(0).getWorkerId()).isEqualTo(2L);
        }
    }

    @Nested
    @DisplayName("calculateScore()")
    class CalculateScore {

        @Test
        @DisplayName("penalizes cancellations, capped at 15 points")
        void cancellationsPenalizedAndCapped() {
            double scoreNoCancellations = WorkerPerformanceService.calculateScore(5.0, 100, 100, 0, 50);
            double scoreManyCancellations = WorkerPerformanceService.calculateScore(5.0, 100, 100, 10, 50);

            assertThat(scoreNoCancellations - scoreManyCancellations).isEqualTo(15.0);
        }

        @Test
        @DisplayName("applies diminishing returns to review count via log scale")
        void reviewCountHasDiminishingReturns() {
            double scoreFewReviews = WorkerPerformanceService.calculateScore(4.0, 80, 80, 0, 2);
            double scoreManyReviews = WorkerPerformanceService.calculateScore(4.0, 80, 80, 0, 200);

            assertThat(scoreManyReviews).isGreaterThan(scoreFewReviews);
            assertThat(scoreManyReviews - scoreFewReviews).isLessThan(10.0);
        }
    }
}
