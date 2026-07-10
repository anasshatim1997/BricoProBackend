package com.bricopro.analytics;

import com.bricopro.analytics.service.WorkerPerformanceReportCacheService;
import com.bricopro.user.entity.WorkerProfile;
import com.bricopro.user.repository.WorkerProfileRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkerPerformanceService {

    private final WorkerProfileRepository workerProfileRepository;
    private final WorkerPerformanceReportCacheService reportCacheService;

    public PerformanceReport getReport(Long workerId) {
        return reportCacheService.getReport(workerId);
    }

    @Cacheable(value = "leaderboard", key = "#limit")
    public List<PerformanceReport> getLeaderboard(int limit) {
        List<WorkerProfile> candidates = workerProfileRepository
                .findAll(PageRequest.of(0, Math.max(limit * 3, 50)))
                .getContent();

        record ScoredProfile(WorkerProfile profile, double score) {}

        List<WorkerProfile> topN = candidates.stream()
                .map(wp -> {
                    double score = calculateScore(
                            wp.getAverageRating().doubleValue(),
                            wp.getTotalMissions() > 0
                                    ? Math.min(100.0, wp.getTotalMissions() * 2.0)
                                    : 0,
                            wp.getResponseRate().doubleValue(),
                            wp.getCancellationCount(),
                            wp.getTotalReviews()
                    );
                    return new ScoredProfile(wp, score);
                })
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .limit(limit)
                .map(ScoredProfile::profile)
                .collect(Collectors.toList());

        return topN.stream()
                .map(wp -> reportCacheService.getReport(wp.getUser().getId()))
                .sorted((a, b) -> Double.compare(b.getPerformanceScore(), a.getPerformanceScore()))
                .collect(Collectors.toList());
    }

    public static double calculateScore(double rating, double completionRate,
                                         double responseRate, int cancellations, int reviews) {
        double ratingScore     = (rating / 5.0) * 35;
        double completionScore = (completionRate / 100.0) * 30;
        double responseScore   = (responseRate / 100.0) * 20;
        double reviewScore     = Math.min(1.0, Math.log10(reviews + 1) / 2.0) * 10;
        double cancelPenalty   = Math.min(15, cancellations * 3.0);
        return ratingScore + completionScore + responseScore + reviewScore - cancelPenalty;
    }

    @Data @Builder
    public static class PerformanceReport {
        private Long workerId;
        private long totalMissions;
        private long completedMissions;
        private long cancelledMissions;
        private double completionRate;
        private double averageRating;
        private int totalReviews;
        private double responseRate;
        private int cancellationCount;
        private BigDecimal currentMonthRevenue;
        private double performanceScore;
        private String tier;
        private boolean isPremium;
    }
}
