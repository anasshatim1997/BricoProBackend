package com.bricopro.analytics;

import com.bricopro.analytics.dto.AnalyticsDtos.*;
import com.bricopro.analytics.service.AnalyticsService;
import com.bricopro.payment.repository.PaymentRepository;
import com.bricopro.task.entity.Task.TaskStatus;
import com.bricopro.task.repository.TaskRepository;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.User.Role;
import com.bricopro.user.entity.User.Status;
import com.bricopro.user.entity.WorkerProfile;
import com.bricopro.user.entity.WorkerProfile.ServiceType;
import com.bricopro.user.repository.UserRepository;
import com.bricopro.user.repository.WorkerProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsService")
class AnalyticsServiceTest {

    @Mock UserRepository userRepository;
    @Mock TaskRepository taskRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock WorkerProfileRepository workerProfileRepository;

    @InjectMocks AnalyticsService analyticsService;

    private User workerUser;
    private User clientUser;
    private WorkerProfile workerProfile;

    @BeforeEach
    void setup() {
        workerUser = User.builder().id(2L).firstName("Fouad").lastName("Mernissi")
                .role(Role.WORKER).status(Status.ACTIVE).build();

        clientUser = User.builder().id(1L).firstName("Nour").lastName("El Alami")
                .role(Role.CLIENT).status(Status.ACTIVE).build();

        workerProfile = WorkerProfile.builder()
                .id(1L).user(workerUser)
                .averageRating(BigDecimal.valueOf(4.5))
                .totalReviews(20).totalMissions(40)
                .build();
    }

    // ─── ADMIN DASHBOARD ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("adminDashboard()")
    class AdminDashboard {

        @BeforeEach
        void stubCommon() {
            when(userRepository.count()).thenReturn(150L);
            when(userRepository.search(eq(Role.WORKER), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(), Pageable.unpaged(), 40L));
            when(userRepository.search(eq(Role.CLIENT), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(), Pageable.unpaged(), 100L));
            when(userRepository.search(isNull(), eq(Status.PENDING), isNull(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(), Pageable.unpaged(), 5L));

            when(taskRepository.count()).thenReturn(300L);
            when(taskRepository.countActiveTasks()).thenReturn(25L);
            when(taskRepository.countByStatus(TaskStatus.COMPLETED)).thenReturn(200L);
            when(taskRepository.countByStatus(TaskStatus.DISPUTED)).thenReturn(3L);
            when(taskRepository.countByStatus(any())).thenReturn(10L); // for all statuses
            when(taskRepository.countByServiceType(any())).thenReturn(30L);

            when(paymentRepository.sumAllPlatformFees()).thenReturn(BigDecimal.valueOf(50000));
            when(paymentRepository.sumPlatformFeeByMonthAndYear(anyInt(), anyInt()))
                    .thenReturn(BigDecimal.valueOf(4000));
        }

        @Test
        @DisplayName("returns correct user totals")
        void userTotals() {
            AdminDashboardResponse res = analyticsService.adminDashboard();

            assertThat(res.getTotalUsers()).isEqualTo(150L);
            assertThat(res.getTotalWorkers()).isEqualTo(40L);
            assertThat(res.getTotalClients()).isEqualTo(100L);
            assertThat(res.getPendingVerifications()).isEqualTo(5L);
        }

        @Test
        @DisplayName("returns correct task counts")
        void taskCounts() {
            AdminDashboardResponse res = analyticsService.adminDashboard();

            assertThat(res.getTotalTasks()).isEqualTo(300L);
            assertThat(res.getActiveTasks()).isEqualTo(25L);
        }

        @Test
        @DisplayName("returns total revenue and monthly revenue")
        void revenueFields() {
            AdminDashboardResponse res = analyticsService.adminDashboard();

            assertThat(res.getTotalRevenue()).isEqualByComparingTo(BigDecimal.valueOf(50000));
            assertThat(res.getMonthlyRevenue()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("tasksByStatus map is populated for all statuses")
        void tasksByStatusPopulated() {
            AdminDashboardResponse res = analyticsService.adminDashboard();

            assertThat(res.getTasksByStatus()).isNotEmpty();
            assertThat(res.getTasksByStatus()).containsKey("COMPLETED");
        }

        @Test
        @DisplayName("tasksByServiceType map is populated")
        void tasksByServiceTypePopulated() {
            AdminDashboardResponse res = analyticsService.adminDashboard();

            assertThat(res.getTasksByServiceType()).isNotEmpty();
            assertThat(res.getTasksByServiceType()).containsKey("PLUMBING");
        }

        @Test
        @DisplayName("handles null total revenue gracefully")
        void nullRevenue() {
            when(paymentRepository.sumAllPlatformFees()).thenReturn(null);

            AdminDashboardResponse res = analyticsService.adminDashboard();

            assertThat(res.getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("revenueByMonth map contains entries for each past month")
        void revenueByMonthMap() {
            AdminDashboardResponse res = analyticsService.adminDashboard();

            assertThat(res.getRevenueByMonth()).isNotEmpty();
            res.getRevenueByMonth().values().forEach(v ->
                    assertThat(v).isGreaterThanOrEqualTo(BigDecimal.ZERO));
        }
    }

    // ─── WORKER DASHBOARD ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("workerDashboard()")
    class WorkerDashboard {

        @Test
        @DisplayName("returns correct mission counts and revenue")
        void correctCounts() {
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(workerProfile));
            when(taskRepository.countByWorkerIdAndStatus(2L, TaskStatus.CONFIRMED)).thenReturn(3L);
            when(taskRepository.countByWorkerIdAndStatus(2L, TaskStatus.STARTED)).thenReturn(2L);
            when(taskRepository.countByWorkerIdAndStatus(2L, TaskStatus.COMPLETED)).thenReturn(35L);
            when(paymentRepository.sumWorkerRevenueByMonth(eq(2L), anyInt(), anyInt()))
                    .thenReturn(BigDecimal.valueOf(3500));
            when(paymentRepository.sumAllWorkerRevenue(2L)).thenReturn(BigDecimal.valueOf(45000));

            WorkerDashboardResponse res = analyticsService.workerDashboard(2L);

            assertThat(res.getTotalMissions()).isEqualTo(40);        // from profile
            assertThat(res.getActiveMissions()).isEqualTo(5L);       // 3 CONFIRMED + 2 STARTED
            assertThat(res.getCompletedMissions()).isEqualTo(35L);
            assertThat(res.getAverageRating()).isEqualTo(4.5);
            assertThat(res.getTotalReviews()).isEqualTo(20);
        }

        @Test
        @DisplayName("handles null revenues — returns zero")
        void nullRevenues() {
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(workerProfile));
            when(taskRepository.countByWorkerIdAndStatus(any(), any())).thenReturn(0L);
            when(paymentRepository.sumWorkerRevenueByMonth(any(), anyInt(), anyInt())).thenReturn(null);
            when(paymentRepository.sumAllWorkerRevenue(any())).thenReturn(null);

            WorkerDashboardResponse res = analyticsService.workerDashboard(2L);

            assertThat(res.getCurrentMonthRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(res.getTotalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("throws when worker profile not found")
        void profileNotFound() {
            when(workerProfileRepository.findByUserId(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> analyticsService.workerDashboard(99L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("revenueByMonth is built month-by-month up to current month")
        void revenueByMonthBuilt() {
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(workerProfile));
            when(taskRepository.countByWorkerIdAndStatus(any(), any())).thenReturn(0L);
            when(paymentRepository.sumWorkerRevenueByMonth(any(), anyInt(), anyInt()))
                    .thenReturn(BigDecimal.valueOf(1000));
            when(paymentRepository.sumAllWorkerRevenue(any())).thenReturn(BigDecimal.valueOf(12000));

            WorkerDashboardResponse res = analyticsService.workerDashboard(2L);

            assertThat(res.getRevenueByMonth()).isNotEmpty();
        }
    }

    // ─── CLIENT DASHBOARD ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("clientDashboard()")
    class ClientDashboard {

        @Test
        @DisplayName("returns correct request counts and spending")
        void correctCounts() {
            when(taskRepository.countByClientId(1L)).thenReturn(15L);
            when(taskRepository.countByClientIdAndStatus(1L, TaskStatus.CONFIRMED)).thenReturn(2L);
            when(taskRepository.countByClientIdAndStatus(1L, TaskStatus.STARTED)).thenReturn(1L);
            when(taskRepository.countByClientIdAndStatus(1L, TaskStatus.COMPLETED)).thenReturn(10L);
            when(taskRepository.countByClientIdAndStatus(1L, TaskStatus.CANCELLED)).thenReturn(2L);
            when(paymentRepository.sumClientSpentByMonth(eq(1L), anyInt(), anyInt()))
                    .thenReturn(BigDecimal.valueOf(1500));
            when(paymentRepository.sumAllClientSpent(1L)).thenReturn(BigDecimal.valueOf(12000));

            ClientDashboardResponse res = analyticsService.clientDashboard(1L);

            assertThat(res.getTotalRequests()).isEqualTo(15L);
            assertThat(res.getActiveRequests()).isEqualTo(3L);  // 2 CONFIRMED + 1 STARTED
            assertThat(res.getCompletedRequests()).isEqualTo(10L);
            assertThat(res.getCancelledRequests()).isEqualTo(2L);
            assertThat(res.getCurrentMonthSpent()).isEqualByComparingTo(BigDecimal.valueOf(1500));
            assertThat(res.getTotalSpent()).isEqualByComparingTo(BigDecimal.valueOf(12000));
        }

        @Test
        @DisplayName("handles null spent values — returns zero")
        void nullSpent() {
            when(taskRepository.countByClientId(1L)).thenReturn(0L);
            when(taskRepository.countByClientIdAndStatus(any(), any())).thenReturn(0L);
            when(paymentRepository.sumClientSpentByMonth(any(), anyInt(), anyInt())).thenReturn(null);
            when(paymentRepository.sumAllClientSpent(any())).thenReturn(null);

            ClientDashboardResponse res = analyticsService.clientDashboard(1L);

            assertThat(res.getCurrentMonthSpent()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(res.getTotalSpent()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}
