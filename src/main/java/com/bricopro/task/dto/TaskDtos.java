package com.bricopro.task.dto;

import com.bricopro.task.entity.Task.CancelledBy;
import com.bricopro.task.entity.Task.TaskStatus;
import com.bricopro.user.dto.UserDtos.UserSummary;
import com.bricopro.user.entity.WorkerProfile.ServiceType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

public class TaskDtos {

    @Data
    @Schema(description = "Request payload for: Create Task.")
    public static class CreateTaskRequest {
        @NotNull
        @Schema(description = "Service category: REPAIRS, PLUMBING, CLEANING, PAINTING, etc.", example = "value")
        private ServiceType serviceType;
        @NotBlank @Size(max = 255)
        @Schema(description = "Short descriptive title", example = "example")
        private String title;
        @NotBlank
        @Schema(description = "Detailed description", example = "example")
        private String description;
        @NotBlank
        @Schema(description = "Street address for the service location", example = "example")
        private String address;
        @Schema(description = "GPS latitude coordinate", example = "0.0")
        private Double latitude;
        @Schema(description = "GPS longitude coordinate", example = "0.0")
        private Double longitude;
        @NotNull @Future
        @Schema(description = "Date the service is scheduled (YYYY-MM-DD)", example = "2025-06-15")
        private LocalDate scheduledDate;
        @NotNull
        @Schema(description = "Scheduled start time (HH:mm)", example = "09:00")
        private LocalTime scheduledStart;
        @Schema(description = "Scheduled end time (HH:mm)", example = "09:00")
        private LocalTime scheduledEnd;
        @Schema(description = "Minimum acceptable budget in MAD", example = "150.00")
        private BigDecimal budgetMin;
        @Schema(description = "Maximum acceptable budget in MAD", example = "150.00")
        private BigDecimal budgetMax;
        @Schema(description = "Flag — marks the task as urgent so it is shown first to workers", example = "false")
        private boolean urgent;
        @Schema(description = "Enable bidding for this task", example = "false")
        private Boolean biddingEnabled;

        @Schema(description = "Deadline for bids (ISO-8601 datetime)", example = "2025-06-20T18:00:00")
        private LocalDateTime biddingDeadline;

        @Schema(description = "Automatically assign the first matching worker", example = "false")
        private Boolean autoAssignEnabled;
    }

    @Data
    @Schema(description = "Response body returned by: Task.")
    public static class TaskResponse {
        @Schema(description = "Unique identifier", example = "1")
        private Long id;
        @Schema(description = "Client", example = "value")
        private UserSummary client;
        @Schema(description = "Worker", example = "value")
        private UserSummary worker;
        @Schema(description = "Service category: REPAIRS, PLUMBING, CLEANING, PAINTING, etc.", example = "value")
        private ServiceType serviceType;
        @Schema(description = "Short descriptive title", example = "example")
        private String title;
        @Schema(description = "Detailed description", example = "example")
        private String description;
        @Schema(description = "Street address for the service location", example = "example")
        private String address;
        @Schema(description = "GPS latitude coordinate", example = "0.0")
        private Double latitude;
        @Schema(description = "GPS longitude coordinate", example = "0.0")
        private Double longitude;
        @Schema(description = "Date the service is scheduled (YYYY-MM-DD)", example = "2025-06-15")
        private LocalDate scheduledDate;
        @Schema(description = "Scheduled start time (HH:mm)", example = "09:00")
        private LocalTime scheduledStart;
        @Schema(description = "Scheduled end time (HH:mm)", example = "09:00")
        private LocalTime scheduledEnd;
        @Schema(description = "Minimum acceptable budget in MAD", example = "150.00")
        private BigDecimal budgetMin;
        @Schema(description = "Maximum acceptable budget in MAD", example = "150.00")
        private BigDecimal budgetMax;
        @Schema(description = "Final price agreed between client and worker in MAD", example = "150.00")
        private BigDecimal agreedPrice;
        @Schema(description = "Current status of the entity", example = "value")
        private TaskStatus status;
        @Schema(description = "Is Urgent", example = "false")
        private boolean isUrgent;
        @Schema(description = "Photo Urls", example = "[]")
        private List<String> photoUrls;
        @Schema(description = "ISO-8601 timestamp when the record was created", example = "2025-06-15")
        private LocalDateTime createdAt;
        @Schema(description = "ISO-8601 timestamp of the last update", example = "2025-06-15")
        private LocalDateTime updatedAt;
        @Schema(description = "Enable bidding for this task", example = "false")
        private Boolean biddingEnabled;

        @Schema(description = "Deadline for bids (ISO-8601 datetime)", example = "2025-06-20T18:00:00")
        private LocalDateTime biddingDeadline;

        @Schema(description = "Automatically assign the first matching worker", example = "false")
        private Boolean autoAssignEnabled;
    }

    @Data
    @Schema(description = "Request payload for: Update Task Status.")
    public static class UpdateTaskStatusRequest {
        @NotNull
        @Schema(description = "Current status of the entity", example = "value")
        private TaskStatus status;
        @Schema(description = "Final price agreed between client and worker in MAD", example = "150.00")
        private BigDecimal agreedPrice;
        @Schema(description = "Cancellation Reason", example = "example")
        private String cancellationReason;
    }

    @Data
    @Schema(description = "Request payload for: Assign Worker.")
    public static class AssignWorkerRequest {
        @NotNull
        @Schema(description = "ID of the assigned or target worker", example = "1")
        private Long workerId;
        @Schema(description = "Final price agreed between client and worker in MAD", example = "150.00")
        private BigDecimal agreedPrice;
    }

    @Data
    @Schema(description = "Request payload for: Create Review.")
    public static class CreateReviewRequest {
        @NotNull @Min(1) @Max(5)
        @Schema(description = "Star rating from 1 (poor) to 5 (excellent)", example = "0")
        private int rating;
        @Size(max = 1000)
        @Schema(description = "Optional written feedback", example = "example")
        private String comment;
    }

    @Data
    @Schema(description = "Response body returned by: Review.")
    public static class ReviewResponse {
        @Schema(description = "Unique identifier", example = "1")
        private Long id;
        @Schema(description = "ID of the related task", example = "1")
        private Long taskId;
        @Schema(description = "Reviewer", example = "value")
        private UserSummary reviewer;
        @Schema(description = "Reviewee", example = "value")
        private UserSummary reviewee;
        @Schema(description = "Star rating from 1 (poor) to 5 (excellent)", example = "0")
        private int rating;
        @Schema(description = "Optional written feedback", example = "example")
        private String comment;
        @Schema(description = "ISO-8601 timestamp when the record was created", example = "2025-06-15")
        private LocalDateTime createdAt;
    }
}
