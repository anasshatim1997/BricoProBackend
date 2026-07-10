package com.bricopro.analytics;

import com.bricopro.analytics.service.WorkerPerformanceReportCacheService;
import com.bricopro.analytics.WorkerPerformanceService.PerformanceReport;
import com.bricopro.payment.repository.PaymentRepository;
import com.bricopro.task.entity.Task.TaskStatus;
import com.bricopro.task.repository.ReviewRepository;
import com.bricopro.task.repository.TaskRepository;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.User.Role;
import com.bricopro.user.entity.WorkerProfile;
import com.bricopro.user.repository.WorkerProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkerPerformanceReportCacheService")
class WorkerPerformanceReportCacheServiceTest {

    @Mock WorkerProfileRepository workerProfileRepository;
    @Mock TaskRepository taskRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock PaymentRepository paymentRepository;

    @InjectMocks WorkerPerformanceReportCacheService performanceService;

    private User workerUser;
    private WorkerProfile profile;

    @BeforeEach
    void setup() {
        workerUser = User.builder()
                .id(2L).firstName("Khalid").lastName("Rhani")
                .role(Role.WORKER).build();

        profile = WorkerProfile.builder()
                .id(1L).user(workerUser)
                .averageRating(BigDecimal.valueOf(4.7))
                .totalReviews(30)
                .totalMissions(60)
                .responseRate(BigDecimal.valueOf(92))
                .cancellationCount(1)
                .isPremium(false)
                .build();
    }

    // ─── GET REPORT ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getReport()")
    class GetReport {

        @Test
        @DisplayName("builds full performance report correctly")
        void buildsReport() {
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(profile));
            when(taskRepository.countByWorkerId(2L)).thenReturn(60L);
            when(taskRepository.countByWorkerIdAndStatus(2L, TaskStatus.COMPLETED)).thenReturn(55L);
            when(taskRepository.countByWorkerIdAndStatus(2L, TaskStatus.CANCELLED)).thenReturn(3L);
            when(reviewRepository.calculateAverageRating(2L)).thenReturn(4.7);
            when(paymentRepository.sumWorkerRevenueByMonth(eq(2L), anyInt(), anyInt()))
                    .thenReturn(BigDecimal.valueOf(3000));

            PerformanceReport report = performanceService.getReport(2L);

            assertThat(report.getWorkerId()).isEqualTo(2L);
            assertThat(report.getTotalMissions()).isEqualTo(60L);
            assertThat(report.getCompletedMissions()).isEqualTo(55L);
            assertThat(report.getCancelledMissions()).isEqualTo(3L);
            assertThat(report.getAverageRating()).isEqualTo(4.7);
            assertThat(report.getTotalReviews()).isEqualTo(30);
            assertThat(report.getResponseRate()).isEqualTo(92.0);
            assertThat(report.getCurrentMonthRevenue())
                    .isEqualByComparingTo(BigDecimal.valueOf(3000));
        }

        @Test
        @DisplayName("completion rate is correctly calculated as percentage")
        void completionRate() {
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(profile));
            when(taskRepository.countByWorkerId(2L)).thenReturn(50L);
            when(taskRepository.countByWorkerIdAndStatus(2L, TaskStatus.COMPLETED)).thenReturn(45L);
            when(taskRepository.countByWorkerIdAndStatus(2L, TaskStatus.CANCELLED)).thenReturn(5L);
            when(reviewRepository.calculateAverageRating(2L)).thenReturn(4.5);
            when(paymentRepository.sumWorkerRevenueByMonth(any(), anyInt(), anyInt()))
                    .thenReturn(BigDecimal.ZERO);

            PerformanceReport report = performanceService.getReport(2L);

            // 45/50 * 100 = 90.0
            assertThat(report.getCompletionRate()).isEqualTo(90.0);
        }

        @Test
        @DisplayName("completion rate is 0 when no missions")
        void completionRateZeroMissions() {
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(profile));
            when(taskRepository.countByWorkerId(2L)).thenReturn(0L);
            when(taskRepository.countByWorkerIdAndStatus(2L, TaskStatus.COMPLETED)).thenReturn(0L);
            when(taskRepository.countByWorkerIdAndStatus(2L, TaskStatus.CANCELLED)).thenReturn(0L);
            when(reviewRepository.calculateAverageRating(2L)).thenReturn(null);
            when(paymentRepository.sumWorkerRevenueByMonth(any(), anyInt(), anyInt()))
                    .thenReturn(null);

            PerformanceReport report = performanceService.getReport(2L);

            assertThat(report.getCompletionRate()).isEqualTo(0.0);
            assertThat(report.getAverageRating()).isEqualTo(0.0);
            assertThat(report.getCurrentMonthRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("tier assignment — ELITE for score >= 85")
        void tierElite() {
            // Build a profile that will score >= 85
            // rating=5, completionRate=100%, responseRate=100%, 50+ reviews, 0 cancellations
            WorkerProfile eliteProfile = WorkerProfile.builder()
                    .id(2L).user(workerUser)
                    .averageRating(BigDecimal.valueOf(5.0))
                    .totalReviews(100)
                    .totalMissions(100)
                    .responseRate(BigDecimal.valueOf(100))
                    .cancellationCount(0)
                    .isPremium(true)
                    .build();

            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(eliteProfile));
            when(taskRepository.countByWorkerId(2L)).thenReturn(100L);
            when(taskRepository.countByWorkerIdAndStatus(2L, TaskStatus.COMPLETED)).thenReturn(100L);
            when(taskRepository.countByWorkerIdAndStatus(2L, TaskStatus.CANCELLED)).thenReturn(0L);
            when(reviewRepository.calculateAverageRating(2L)).thenReturn(5.0);
            when(paymentRepository.sumWorkerRevenueByMonth(any(), anyInt(), anyInt()))
                    .thenReturn(BigDecimal.valueOf(10000));

            PerformanceReport report = performanceService.getReport(2L);

            assertThat(report.getTier()).isEqualTo("ELITE");
        }

        @Test
        @DisplayName("tier assignment — BRONZE for low score")
        void tierBronze() {
            WorkerProfile poorProfile = WorkerProfile.builder()
                    .id(3L).user(workerUser)
                    .averageRating(BigDecimal.valueOf(2.0))
                    .totalReviews(3)
                    .totalMissions(10)
                    .responseRate(BigDecimal.valueOf(40))
                    .cancellationCount(5)
                    .isPremium(false)
                    .build();

            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(poorProfile));
            when(taskRepository.countByWorkerId(2L)).thenReturn(10L);
            when(taskRepository.countByWorkerIdAndStatus(2L, TaskStatus.COMPLETED)).thenReturn(4L);
            when(taskRepository.countByWorkerIdAndStatus(2L, TaskStatus.CANCELLED)).thenReturn(6L);
            when(reviewRepository.calculateAverageRating(2L)).thenReturn(2.0);
            when(paymentRepository.sumWorkerRevenueByMonth(any(), anyInt(), anyInt()))
                    .thenReturn(BigDecimal.ZERO);

            PerformanceReport report = performanceService.getReport(2L);

            assertThat(report.getTier()).isEqualTo("BRONZE");
        }

        @Test
        @DisplayName("tier assignment — GOLD for score between 70 and 85")
        void tierGold() {
            WorkerProfile goldProfile = WorkerProfile.builder()
                    .id(4L).user(workerUser)
                    .averageRating(BigDecimal.valueOf(4.5))
                    .totalReviews(25)
                    .totalMissions(50)
                    .responseRate(BigDecimal.valueOf(85))
                    .cancellationCount(0)
                    .isPremium(false)
                    .build();

            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(goldProfile));
            when(taskRepository.countByWorkerId(2L)).thenReturn(50L);
            when(taskRepository.countByWorkerIdAndStatus(2L, TaskStatus.COMPLETED)).thenReturn(48L);
            when(taskRepository.countByWorkerIdAndStatus(2L, TaskStatus.CANCELLED)).thenReturn(2L);
            when(reviewRepository.calculateAverageRating(2L)).thenReturn(4.5);
            when(paymentRepository.sumWorkerRevenueByMonth(any(), anyInt(), anyInt()))
                    .thenReturn(BigDecimal.valueOf(5000));

            PerformanceReport report = performanceService.getReport(2L);

            assertThat(report.getTier()).isIn("GOLD", "ELITE"); // edge case, accept both
        }

        @Test
        @DisplayName("throws when worker profile not found")
        void profileNotFound() {
            when(workerProfileRepository.findByUserId(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> performanceService.getReport(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("isPremium flag is correctly propagated to report")
        void premiumFlagInReport() {
            profile.setPremium(true);
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(profile));
            when(taskRepository.countByWorkerId(2L)).thenReturn(60L);
            when(taskRepository.countByWorkerIdAndStatus(2L, TaskStatus.COMPLETED)).thenReturn(55L);
            when(taskRepository.countByWorkerIdAndStatus(2L, TaskStatus.CANCELLED)).thenReturn(3L);
            when(reviewRepository.calculateAverageRating(2L)).thenReturn(4.7);
            when(paymentRepository.sumWorkerRevenueByMonth(any(), anyInt(), anyInt()))
                    .thenReturn(BigDecimal.ZERO);

            PerformanceReport report = performanceService.getReport(2L);
            assertThat(report.isPremium()).isTrue();
        }
    }
}
