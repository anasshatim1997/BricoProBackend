package com.bricopro.analytics.service;

import com.bricopro.payment.repository.PaymentRepository;
import com.bricopro.task.entity.Task.TaskStatus;
import com.bricopro.task.repository.TaskRepository;
import com.bricopro.task.repository.ReviewRepository;
import com.bricopro.user.entity.WorkerProfile;
import com.bricopro.user.repository.WorkerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class WorkerPerformanceReportCacheService {

    private final WorkerProfileRepository workerProfileRepository;
    private final TaskRepository          taskRepository;
    private final ReviewRepository        reviewRepository;
    private final PaymentRepository       paymentRepository;

    @Cacheable(value = "workerPerformance", key = "#workerId")
    public WorkerPerformanceService.PerformanceReport getReport(Long workerId) {
        WorkerProfile profile = workerProfileRepository.findByUserId(workerId)
                .orElseThrow(() -> new IllegalArgumentException("Worker not found"));

        int currentMonth = LocalDate.now().getMonthValue();
        int currentYear  = LocalDate.now().getYear();

        long totalMissions  = taskRepository.countByWorkerId(workerId);
        long completedCount = taskRepository.countByWorkerIdAndStatus(workerId, TaskStatus.COMPLETED);
        long cancelledCount = taskRepository.countByWorkerIdAndStatus(workerId, TaskStatus.CANCELLED);

        double completionRate = totalMissions > 0
                ? (double) completedCount / totalMissions * 100 : 0;

        Double avgRating = reviewRepository.calculateAverageRating(workerId);
        BigDecimal revenue = paymentRepository.sumWorkerRevenueByMonth(workerId, currentMonth, currentYear);

        double performanceScore = WorkerPerformanceService.calculateScore(
                profile.getAverageRating().doubleValue(),
                completionRate,
                profile.getResponseRate().doubleValue(),
                profile.getCancellationCount(),
                profile.getTotalReviews()
        );

        String tier = performanceScore >= 85 ? "ELITE"
                : performanceScore >= 70 ? "GOLD"
                : performanceScore >= 55 ? "SILVER"
                : "BRONZE";

        return WorkerPerformanceService.PerformanceReport.builder()
                .workerId(workerId)
                .totalMissions(totalMissions)
                .completedMissions(completedCount)
                .cancelledMissions(cancelledCount)
                .completionRate(Math.round(completionRate * 10.0) / 10.0)
                .averageRating(avgRating != null ? avgRating : 0.0)
                .totalReviews(profile.getTotalReviews())
                .responseRate(profile.getResponseRate().doubleValue())
                .cancellationCount(profile.getCancellationCount())
                .currentMonthRevenue(revenue != null ? revenue : BigDecimal.ZERO)
                .performanceScore(Math.round(performanceScore * 10.0) / 10.0)
                .tier(tier)
                .isPremium(profile.isPremium())
                .build();
    }
}
