package com.bricopro.bidding.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class BidDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateBidRequest {
        private Long taskId;
        private BigDecimal amount;
        private String message;
        private Integer estimatedDurationHours;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateBidRequest {
        private BigDecimal amount;
        private String message;
        private Integer estimatedDurationHours;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CounterBidRequest {
        private BigDecimal amount;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviseBidRequest {
        private BigDecimal amount;
        private String message;
        private Integer estimatedDurationHours;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BidResponse {
        private Long id;
        private Long taskId;
        private Long workerId;
        private BigDecimal amount;
        private String message;
        private Integer estimatedDurationHours;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime expiresAt;
        private String workerName;
        private Double workerRating;
        private Long parentBidId;
        private List<BidResponse> children;
    }
}