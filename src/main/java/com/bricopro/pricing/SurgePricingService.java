package com.bricopro.pricing;

import com.bricopro.task.entity.Task;
import com.bricopro.task.repository.TaskRepository;
import com.bricopro.user.entity.WorkerProfile.ServiceType;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Surge Pricing Service", description = "Business logic for Surge Pricing Service")
@Service
@RequiredArgsConstructor
public class SurgePricingService {

    @Schema(description = "Task Repository", example = "value")
    private final TaskRepository taskRepository;

    private static final BigDecimal URGENT_MULTIPLIER       = new BigDecimal("1.25");
    private static final BigDecimal PEAK_HOUR_MULTIPLIER    = new BigDecimal("1.15");
    private static final BigDecimal HIGH_DEMAND_MULTIPLIER  = new BigDecimal("1.20");
    private static final BigDecimal WEEKEND_MULTIPLIER      = new BigDecimal("1.10");

    private static final Map<ServiceType, Integer> DEMAND_THRESHOLDS = Map.of(
            ServiceType.REPAIRS,      5,
            ServiceType.PLUMBING,     3,
            ServiceType.CLEANING,     8,
            ServiceType.MOVING,       4,
            ServiceType.PAINTING,     6,
            ServiceType.CONSTRUCTION, 3,
            ServiceType.OUTDOOR,      5,
            ServiceType.DECORATION,   7,
            ServiceType.ASSEMBLY,     6
    );

    /**
     * Calculate.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public SurgeResult calculate(BigDecimal basePrice, ServiceType serviceType, boolean isUrgent) {
        BigDecimal multiplier = BigDecimal.ONE;
        StringBuilder reason  = new StringBuilder();

        if (isUrgent) {
            multiplier = multiplier.multiply(URGENT_MULTIPLIER);
            reason.append("+25% urgence ");
        }

        if (isPeakHour()) {
            multiplier = multiplier.multiply(PEAK_HOUR_MULTIPLIER);
            reason.append("+15% heure de pointe ");
        }

        if (isWeekend()) {
            multiplier = multiplier.multiply(WEEKEND_MULTIPLIER);
            reason.append("+10% weekend ");
        }

        if (isHighDemand(serviceType)) {
            multiplier = multiplier.multiply(HIGH_DEMAND_MULTIPLIER);
            reason.append("+20% forte demande ");
        }

        BigDecimal surgedPrice = basePrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);

        return SurgeResult.builder()
                .originalPrice(basePrice)
                .surgedPrice(surgedPrice)
                .multiplier(multiplier)
                .surgeActive(!multiplier.equals(BigDecimal.ONE))
                .reason(reason.toString().trim())
                .build();
    }

    /**
     * Estimate For Task.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public SurgeResult estimateForTask(Task task) {
        BigDecimal base = task.getBudgetMin() != null ? task.getBudgetMin() : BigDecimal.ZERO;
        return calculate(base, task.getServiceType(), task.isUrgent());
    }

    private boolean isPeakHour() {
        LocalTime now = LocalTime.now();
        return (now.isAfter(LocalTime.of(8, 0)) && now.isBefore(LocalTime.of(10, 0)))
                || (now.isAfter(LocalTime.of(17, 0)) && now.isBefore(LocalTime.of(20, 0)));
    }

    private boolean isWeekend() {
        java.time.DayOfWeek day = java.time.LocalDate.now().getDayOfWeek();
        return day == java.time.DayOfWeek.SATURDAY || day == java.time.DayOfWeek.SUNDAY;
    }

private boolean isHighDemand(ServiceType serviceType) {
        long searchingCount = taskRepository.countByStatus(
                com.bricopro.task.entity.Task.TaskStatus.SEARCHING);
        int threshold = DEMAND_THRESHOLDS.getOrDefault(serviceType, 5);
        return searchingCount >= threshold;
    }

    @Data
    @lombok.Builder
    @Schema(description = "Surge Result")
    public static class SurgeResult {
        @Schema(description = "Original Price", example = "150.00")
        private BigDecimal originalPrice;
        @Schema(description = "Surged Price", example = "150.00")
        private BigDecimal surgedPrice;
        @Schema(description = "Multiplier", example = "150.00")
        private BigDecimal multiplier;
        @Schema(description = "Surge Active", example = "false")
        private boolean surgeActive;
        @Schema(description = "Reason", example = "example")
        private String reason;
    }
}
