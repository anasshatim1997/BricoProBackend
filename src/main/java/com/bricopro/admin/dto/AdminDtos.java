package com.bricopro.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;

public class AdminDtos {

    @Data
    @Schema(description = "Request payload for: Verify Worker.")
    public static class VerifyWorkerRequest {
        @Schema(description = "Notes", example = "example")
        private String notes;
    }

    @Data
    @Schema(description = "Request payload for: Reject.")
    public static class RejectRequest {
        @NotBlank
        @Schema(description = "Reason", example = "example")
        private String reason;
    }

    @Data
    @Schema(description = "Request payload for: Suspend.")
    public static class SuspendRequest {
        @NotBlank
        @Schema(description = "Reason", example = "example")
        private String reason;
    }

    @Data
    @Schema(description = "Request payload for: Resolve Dispute.")
    public static class ResolveDisputeRequest {
        @NotNull
        @Schema(description = "Resolution", example = "value")
        private DisputeResolution resolution;
        @NotBlank
        @Schema(description = "Reason", example = "example")
        private String reason;

        public enum DisputeResolution { FAVOUR_CLIENT, FAVOUR_WORKER, REFUND, SPLIT }
    }

@Data
    @Schema(description = "Request payload for: Assign Task.")
    public static class AssignTaskRequest {
        @NotNull
        @Schema(description = "ID of the assigned or target worker", example = "1")
        private Long workerId;
        @Schema(description = "Final price agreed between client and worker in MAD", example = "150.00")
        private BigDecimal agreedPrice;
        @Schema(description = "Notes", example = "example")
        private String notes;
    }

    @Data
    @Schema(description = "Data transfer object: Worker Verification.")
    public static class WorkerVerificationDto {
        @Schema(description = "ID of the target user", example = "1")
        private Long userId;
        @Schema(description = "First name of the user", example = "example")
        private String firstName;
        @Schema(description = "Last name of the user", example = "example")
        private String lastName;
        @Schema(description = "Phone number in international format (e.g. +212XXXXXXXXX)", example = "example")
        private String phone;
        @Schema(description = "Email address of the user", example = "example")
        private String email;
        @Schema(description = "Cin Document Url", example = "example")
        private String cinDocumentUrl;
        @Schema(description = "Registered At", example = "2025-06-15")
        private LocalDateTime registeredAt;
    }

    @Data
    @Schema(description = "Data transfer object: Disputed Task.")
    public static class DisputedTaskDto {
        @Schema(description = "ID of the related task", example = "1")
        private Long taskId;
        @Schema(description = "Short descriptive title", example = "example")
        private String title;
        @Schema(description = "Client Name", example = "example")
        private String clientName;
        @Schema(description = "Worker Name", example = "example")
        private String workerName;
        @Schema(description = "Cancellation Reason", example = "example")
        private String cancellationReason;
        @Schema(description = "ISO-8601 timestamp when the record was created", example = "2025-06-15")
        private LocalDateTime createdAt;
    }

    @Data
    @Schema(description = "Data transfer object: User Admin.")
    public static class UserAdminDto {
        @Schema(description = "Unique identifier", example = "1")
        private Long id;
        @Schema(description = "First name of the user", example = "example")
        private String firstName;
        @Schema(description = "Last name of the user", example = "example")
        private String lastName;
        @Schema(description = "Email address of the user", example = "example")
        private String email;
        @Schema(description = "Phone number in international format (e.g. +212XXXXXXXXX)", example = "example")
        private String phone;
        @Schema(description = "User role: CLIENT, WORKER, or ADMIN", example = "example")
        private String role;
        @Schema(description = "Current status of the entity", example = "example")
        private String status;
        @Schema(description = "Verified", example = "false")
        private boolean verified;
        @Schema(description = "ISO-8601 timestamp when the record was created", example = "2025-06-15")
        private LocalDateTime createdAt;
    }

    @Data
    @Schema(description = "Response body returned by: Action.")
    public static class ActionResponse {
        @Schema(description = "Human-readable response message", example = "example")
        private String message;
        @Schema(description = "Success", example = "false")
        private boolean success;

        public ActionResponse(String message, boolean success) {
            this.message = message;
            this.success = success;
        }
    }
}