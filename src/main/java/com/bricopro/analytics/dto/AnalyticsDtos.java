package com.bricopro.analytics.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;

public class AnalyticsDtos {

    @Data @Builder
    @Schema(description = "Response body returned by: Admin Dashboard.")
    public static class AdminDashboardResponse {
        @Schema(description = "Total Users", example = "value")
        private long totalUsers;
        @Schema(description = "Total Workers", example = "value")
        private long totalWorkers;
        @Schema(description = "Total Clients", example = "value")
        private long totalClients;
        @Schema(description = "Pending Verifications", example = "value")
        private long pendingVerifications;
        @Schema(description = "Total Tasks", example = "value")
        private long totalTasks;
        @Schema(description = "Active Tasks", example = "value")
        private long activeTasks;
        @Schema(description = "Completed Tasks", example = "value")
        private long completedTasks;
        @Schema(description = "Disputed Tasks", example = "value")
        private long disputedTasks;
        @Schema(description = "Total Revenue", example = "150.00")
        private BigDecimal totalRevenue;
        @Schema(description = "Monthly Revenue", example = "150.00")
        private BigDecimal monthlyRevenue;
        @Schema(description = "Tasks By Service Type", example = "value")
        private Map<String, Long> tasksByServiceType;
        @Schema(description = "Tasks By Status", example = "value")
        private Map<String, Long> tasksByStatus;
        @Schema(description = "Revenue By Month", example = "value")
        private Map<String, BigDecimal> revenueByMonth;
    }

    @Data @Builder
    @Schema(description = "Response body returned by: Worker Dashboard.")
    public static class WorkerDashboardResponse {
        @Schema(description = "Total Missions", example = "value")
        private long totalMissions;
        @Schema(description = "Active Missions", example = "value")
        private long activeMissions;
        @Schema(description = "Completed Missions", example = "value")
        private long completedMissions;
        @Schema(description = "Average Rating", example = "value")
        private double averageRating;
        @Schema(description = "Total Reviews", example = "0")
        private int totalReviews;
        @Schema(description = "Current Month Revenue", example = "150.00")
        private BigDecimal currentMonthRevenue;
        @Schema(description = "Total Revenue", example = "150.00")
        private BigDecimal totalRevenue;
        @Schema(description = "Revenue By Month", example = "value")
        private Map<String, BigDecimal> revenueByMonth;
    }

    @Data @Builder
    @Schema(description = "Response body returned by: Client Dashboard.")
    public static class ClientDashboardResponse {
        @Schema(description = "Total Requests", example = "value")
        private long totalRequests;
        @Schema(description = "Active Requests", example = "value")
        private long activeRequests;
        @Schema(description = "Completed Requests", example = "value")
        private long completedRequests;
        @Schema(description = "Cancelled Requests", example = "value")
        private long cancelledRequests;
        @Schema(description = "Total Spent", example = "150.00")
        private BigDecimal totalSpent;
        @Schema(description = "Current Month Spent", example = "150.00")
        private BigDecimal currentMonthSpent;
    }
}