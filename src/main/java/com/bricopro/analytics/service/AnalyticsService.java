package com.bricopro.analytics.service;

import com.bricopro.analytics.dto.AnalyticsDtos.AdminDashboardResponse;
import com.bricopro.analytics.dto.AnalyticsDtos.ClientDashboardResponse;
import com.bricopro.analytics.dto.AnalyticsDtos.WorkerDashboardResponse;
import com.bricopro.home.dto.ServiceDto;
import com.bricopro.payment.repository.PaymentRepository;
import com.bricopro.service.ServiceCategoryService;
import com.bricopro.task.entity.Task.TaskStatus;
import com.bricopro.task.repository.TaskRepository;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.User.Role;
import com.bricopro.user.entity.User.Status;
import com.bricopro.user.entity.WorkerProfile;
import com.bricopro.user.entity.WorkerProfile.ServiceType;
import com.bricopro.user.repository.UserRepository;
import com.bricopro.user.repository.WorkerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final PaymentRepository paymentRepository;
    private final WorkerProfileRepository workerProfileRepository;
    private final ServiceCategoryService serviceCategoryService;

    public AdminDashboardResponse adminDashboard() {
        int currentMonth = LocalDate.now().getMonthValue();
        int currentYear = LocalDate.now().getYear();

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (TaskStatus s : TaskStatus.values()) {
            byStatus.put(s.name(), taskRepository.countByStatus(s));
        }

        Map<String, Long> byServiceType = new LinkedHashMap<>();
        for (ServiceType s : ServiceType.values()) {
            byServiceType.put(s.name(), taskRepository.countByServiceType(s));
        }

        BigDecimal totalRevenue = paymentRepository.sumAllPlatformFees();
        BigDecimal monthlyRevenue = paymentRepository.sumPlatformFeeByMonthAndYear(currentMonth, currentYear);

        Map<String, BigDecimal> revenueByMonth = new LinkedHashMap<>();
        for (int m = 1; m <= currentMonth; m++) {
            BigDecimal rev = paymentRepository.sumPlatformFeeByMonthAndYear(m, currentYear);
            revenueByMonth.put(monthLabel(m, currentYear), rev != null ? rev : BigDecimal.ZERO);
        }

        return AdminDashboardResponse.builder()
                .totalUsers(userRepository.count())
                .totalWorkers(userRepository.search(Role.WORKER, null, null, Pageable.unpaged()).getTotalElements())
                .totalClients(userRepository.search(Role.CLIENT, null, null, Pageable.unpaged()).getTotalElements())
                .pendingVerifications(userRepository.search(null, Status.PENDING, null, Pageable.unpaged()).getTotalElements())
                .totalTasks(taskRepository.count())
                .activeTasks(taskRepository.countActiveTasks())
                .completedTasks(taskRepository.countByStatus(TaskStatus.COMPLETED))
                .disputedTasks(taskRepository.countByStatus(TaskStatus.DISPUTED))
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .monthlyRevenue(monthlyRevenue != null ? monthlyRevenue : BigDecimal.ZERO)
                .tasksByStatus(byStatus)
                .tasksByServiceType(byServiceType)
                .revenueByMonth(revenueByMonth)
                .build();
    }

    public WorkerDashboardResponse workerDashboard(Long userId) {
        int currentMonth = LocalDate.now().getMonthValue();
        int currentYear = LocalDate.now().getYear();

        WorkerProfile profile = workerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Worker profile not found"));

        long active = taskRepository.countByWorkerIdAndStatus(userId, TaskStatus.CONFIRMED)
                + taskRepository.countByWorkerIdAndStatus(userId, TaskStatus.STARTED);
        long completed = taskRepository.countByWorkerIdAndStatus(userId, TaskStatus.COMPLETED);

        BigDecimal currentMonthRevenue = paymentRepository.sumWorkerRevenueByMonth(userId, currentMonth, currentYear);
        BigDecimal totalRevenue = paymentRepository.sumAllWorkerRevenue(userId);

        Map<String, BigDecimal> revenueByMonth = new LinkedHashMap<>();
        for (int m = 1; m <= currentMonth; m++) {
            BigDecimal rev = paymentRepository.sumWorkerRevenueByMonth(userId, m, currentYear);
            revenueByMonth.put(monthLabel(m, currentYear), rev != null ? rev : BigDecimal.ZERO);
        }

        return WorkerDashboardResponse.builder()
                .totalMissions(profile.getTotalMissions())
                .activeMissions(active)
                .completedMissions(completed)
                .averageRating(profile.getAverageRating().doubleValue())
                .totalReviews(profile.getTotalReviews())
                .currentMonthRevenue(currentMonthRevenue != null ? currentMonthRevenue : BigDecimal.ZERO)
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .revenueByMonth(revenueByMonth)
                .build();
    }

    public ClientDashboardResponse clientDashboard(Long userId) {
        int currentMonth = LocalDate.now().getMonthValue();
        int currentYear = LocalDate.now().getYear();

        long total = taskRepository.countByClientId(userId);
        long active = taskRepository.countByClientIdAndStatus(userId, TaskStatus.CONFIRMED)
                + taskRepository.countByClientIdAndStatus(userId, TaskStatus.STARTED);
        long completed = taskRepository.countByClientIdAndStatus(userId, TaskStatus.COMPLETED);
        long cancelled = taskRepository.countByClientIdAndStatus(userId, TaskStatus.CANCELLED);

        BigDecimal monthSpent = paymentRepository.sumClientSpentByMonth(userId, currentMonth, currentYear);
        BigDecimal totalSpent = paymentRepository.sumAllClientSpent(userId);

        return ClientDashboardResponse.builder()
                .totalRequests(total)
                .activeRequests(active)
                .completedRequests(completed)
                .cancelledRequests(cancelled)
                .currentMonthSpent(monthSpent != null ? monthSpent : BigDecimal.ZERO)
                .totalSpent(totalSpent != null ? totalSpent : BigDecimal.ZERO)
                .build();
    }

    public List<ServiceDto> getTopServicesForClient(Long clientId) {
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found"));
        List<Object[]> results = taskRepository.countServiceTypesForClient(client);
        return results.stream()
                .map(row -> {
                    ServiceType type = (ServiceType) row[0];
                    return serviceCategoryService.findByKey(type.name());
                })
                .filter(dto -> dto != null)
                .limit(3)
                .collect(Collectors.toList());
    }

    private String monthLabel(int month, int year) {
        return String.format("%02d/%d", month, year);
    }
}