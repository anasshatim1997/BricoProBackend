package com.bricopro.estimation;

import com.bricopro.pricing.SurgePricingService;
import com.bricopro.user.entity.WorkerProfile.ServiceType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Price Estimation Service", description = "Business logic for Price Estimation Service")
@Service
@RequiredArgsConstructor
public class PriceEstimationService {

    @Schema(description = "Surge Pricing Service", example = "value")
    private final SurgePricingService surgePricingService;

private static final Map<ServiceType, BigDecimal[]> BASE_RATES = Map.of(
        ServiceType.REPAIRS,      new BigDecimal[]{new BigDecimal("80"),  new BigDecimal("150")},
        ServiceType.ASSEMBLY,     new BigDecimal[]{new BigDecimal("60"),  new BigDecimal("120")},
        ServiceType.MOVING,       new BigDecimal[]{new BigDecimal("200"), new BigDecimal("500")},
        ServiceType.CLEANING,     new BigDecimal[]{new BigDecimal("50"),  new BigDecimal("100")},
        ServiceType.PAINTING,     new BigDecimal[]{new BigDecimal("80"),  new BigDecimal("180")},
        ServiceType.CONSTRUCTION, new BigDecimal[]{new BigDecimal("100"), new BigDecimal("250")},
        ServiceType.OUTDOOR,      new BigDecimal[]{new BigDecimal("70"),  new BigDecimal("150")},
        ServiceType.DECORATION,   new BigDecimal[]{new BigDecimal("90"),  new BigDecimal("200")},
        ServiceType.PLUMBING,     new BigDecimal[]{new BigDecimal("100"), new BigDecimal("220")}
    );

    private static final Map<String, BigDecimal> ROOM_MULTIPLIERS = Map.of(
        "STUDIO",      new BigDecimal("1.0"),
        "1_BEDROOM",   new BigDecimal("1.2"),
        "2_BEDROOMS",  new BigDecimal("1.5"),
        "3_BEDROOMS",  new BigDecimal("2.0"),
        "4_PLUS",      new BigDecimal("2.8"),
        "VILLA",       new BigDecimal("3.5")
    );

    private static final Map<String, BigDecimal> COMPLEXITY_MULTIPLIERS = Map.of(
        "SIMPLE",  new BigDecimal("1.0"),
        "MEDIUM",  new BigDecimal("1.4"),
        "COMPLEX", new BigDecimal("2.0")
    );

    /**
     * Estimate.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public EstimationResult estimate(EstimationRequest req) {
        BigDecimal[] baseRange = BASE_RATES.getOrDefault(req.getServiceType(),
                new BigDecimal[]{new BigDecimal("80"), new BigDecimal("150")});

        BigDecimal minRate = baseRange[0];
        BigDecimal maxRate = baseRange[1];

        BigDecimal roomMultiplier = ROOM_MULTIPLIERS.getOrDefault(req.getPropertySize(), BigDecimal.ONE);
        BigDecimal complexityMultiplier = COMPLEXITY_MULTIPLIERS.getOrDefault(req.getComplexity(), BigDecimal.ONE);

        BigDecimal estMin = minRate.multiply(roomMultiplier).multiply(complexityMultiplier)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal estMax = maxRate.multiply(roomMultiplier).multiply(complexityMultiplier)
                .setScale(2, RoundingMode.HALF_UP);

if (req.isUrgent()) {
            SurgePricingService.SurgeResult surge = surgePricingService.calculate(
                    estMin, req.getServiceType(), true);
            estMin = surge.getSurgedPrice();
            estMax = surgePricingService.calculate(estMax, req.getServiceType(), true).getSurgedPrice();
        }

        BigDecimal materialCost = req.isMaterialsIncluded()
                ? estMax.multiply(new BigDecimal("0.30")).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return EstimationResult.builder()
                .minPrice(estMin)
                .maxPrice(estMax)
                .estimatedMaterialCost(materialCost)
                .totalMin(estMin.add(materialCost))
                .totalMax(estMax.add(materialCost))
                .currency("MAD")
                .surgeApplied(req.isUrgent())
                .disclaimer("Prix estimatif. Le prestataire fixera le prix définitif après évaluation.")
                .build();
    }

    @Data
    @Schema(description = "Request payload for: Estimation.")
    public static class EstimationRequest {
        @NotNull
        @Schema(description = "Service category: REPAIRS, PLUMBING, CLEANING, PAINTING, etc.", example = "value")
        private ServiceType serviceType;
        private String propertySize = "2_BEDROOMS";
        private String complexity   = "MEDIUM";
        @Schema(description = "Flag — marks the task as urgent so it is shown first to workers", example = "false")
        private boolean urgent;
        @Schema(description = "Materials Included", example = "false")
        private boolean materialsIncluded;
    }

    @Data
    @lombok.Builder
    @Schema(description = "Estimation Result")
    public static class EstimationResult {
        @Schema(description = "Min Price", example = "150.00")
        private BigDecimal minPrice;
        @Schema(description = "Max Price", example = "150.00")
        private BigDecimal maxPrice;
        @Schema(description = "Estimated Material Cost", example = "150.00")
        private BigDecimal estimatedMaterialCost;
        @Schema(description = "Total Min", example = "150.00")
        private BigDecimal totalMin;
        @Schema(description = "Total Max", example = "150.00")
        private BigDecimal totalMax;
        @Schema(description = "Currency", example = "example")
        private String currency;
        @Schema(description = "Surge Applied", example = "false")
        private boolean surgeApplied;
        @Schema(description = "Disclaimer", example = "example")
        private String disclaimer;
    }
}
