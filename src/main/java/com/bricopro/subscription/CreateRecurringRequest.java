package com.bricopro.subscription;

import com.bricopro.user.entity.WorkerProfile;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class CreateRecurringRequest {
    @Schema(description = "Service category: REPAIRS, PLUMBING, CLEANING, PAINTING, etc.", example = "value")
    private WorkerProfile.ServiceType serviceType;
    @Schema(description = "Short descriptive title", example = "example")
    private String title;
    @Schema(description = "Detailed description", example = "example")
    private String description;
    @Schema(description = "Street address for the service location", example = "example")
    private String address;
    private RecurringTask.Frequency frequency;
    @Schema(description = "Preferred Time", example = "09:00")
    private LocalTime preferredTime;
    @Schema(description = "Start Date", example = "2025-06-15")
    private LocalDate startDate;
    @Schema(description = "End Date", example = "2025-06-15")
    private LocalDate endDate;
}
