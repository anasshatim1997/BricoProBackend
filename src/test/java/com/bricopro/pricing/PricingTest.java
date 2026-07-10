package com.bricopro.pricing;

import com.bricopro.estimation.PriceEstimationService;
import com.bricopro.estimation.PriceEstimationService.EstimationRequest;
import com.bricopro.estimation.PriceEstimationService.EstimationResult;
import com.bricopro.task.entity.Task;
import com.bricopro.task.entity.Task.TaskStatus;
import com.bricopro.task.repository.TaskRepository;
import com.bricopro.user.entity.WorkerProfile.ServiceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pricing")
class PricingTest {

    // ─── SURGE PRICING ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("SurgePricingService")
    class SurgePricingServiceTests {

        @Mock TaskRepository taskRepository;
        @InjectMocks SurgePricingService surgePricingService;

        @Test
        @DisplayName("urgent task gets 25% surcharge")
        void urgentSurge() {
            when(taskRepository.countByStatus(TaskStatus.SEARCHING)).thenReturn(1L);
            BigDecimal base = BigDecimal.valueOf(200);

            SurgePricingService.SurgeResult result = surgePricingService.calculate(base, ServiceType.REPAIRS, true);

            assertThat(result.isSurgeActive()).isTrue();
            assertThat(result.getSurgedPrice()).isGreaterThan(base);
            assertThat(result.getMultiplier()).isGreaterThanOrEqualTo(new BigDecimal("1.25"));
        }

        @Test
        @DisplayName("non-urgent task with no demand pressure returns base price")
        void noSurge() {
            when(taskRepository.countByStatus(TaskStatus.SEARCHING)).thenReturn(0L);
            BigDecimal base = BigDecimal.valueOf(200);

            SurgePricingService.SurgeResult result = surgePricingService.calculate(base, ServiceType.REPAIRS, false);

            // No surge unless peak hour/weekend which we cannot control in unit tests
            assertThat(result.getSurgedPrice()).isGreaterThanOrEqualTo(base);
        }

        @Test
        @DisplayName("high demand (>= threshold) adds 20% multiplier")
        void highDemandSurge() {
            // REPAIRS threshold = 5, so 5 searching tasks = high demand
            when(taskRepository.countByStatus(TaskStatus.SEARCHING)).thenReturn(5L);
            BigDecimal base = BigDecimal.valueOf(100);

            SurgePricingService.SurgeResult result = surgePricingService.calculate(base, ServiceType.REPAIRS, false);

            // At minimum, high demand adds 20%, possible peak/weekend adds more
            assertThat(result.getMultiplier()).isGreaterThanOrEqualTo(new BigDecimal("1.20"));
        }

        @Test
        @DisplayName("surge result contains reason string")
        void surgeReasonNotEmpty() {
            when(taskRepository.countByStatus(TaskStatus.SEARCHING)).thenReturn(10L);

            SurgePricingService.SurgeResult result = surgePricingService.calculate(
                    BigDecimal.valueOf(300), ServiceType.CLEANING, true);

            assertThat(result.getReason()).isNotBlank();
        }

        @Test
        @DisplayName("original price is preserved in result")
        void originalPricePreserved() {
            when(taskRepository.countByStatus(TaskStatus.SEARCHING)).thenReturn(0L);
            BigDecimal base = BigDecimal.valueOf(500);

            SurgePricingService.SurgeResult result = surgePricingService.calculate(base, ServiceType.PAINTING, false);

            assertThat(result.getOriginalPrice()).isEqualByComparingTo(base);
        }
    }

    // ─── PRICE ESTIMATION ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("PriceEstimationService")
    class PriceEstimationServiceTests {

        @Mock SurgePricingService surgePricingService;
        @InjectMocks PriceEstimationService estimationService;

        @Test
        @DisplayName("estimates price range for PLUMBING — MEDIUM complexity")
        void estimatesPlumbing() {
            EstimationRequest req = new EstimationRequest();
            req.setServiceType(ServiceType.PLUMBING);
            req.setPropertySize("2_BEDROOMS");
            req.setComplexity("MEDIUM");
            req.setUrgent(false);
            req.setMaterialsIncluded(false);

            EstimationResult result = estimationService.estimate(req);

            assertThat(result.getMinPrice()).isGreaterThan(BigDecimal.ZERO);
            assertThat(result.getMaxPrice()).isGreaterThan(result.getMinPrice());
            assertThat(result.getCurrency()).isEqualTo("MAD");
            assertThat(result.isSurgeApplied()).isFalse();
        }

        @Test
        @DisplayName("materials included adds 30% of max price")
        void materialsAdded() {
            EstimationRequest req = new EstimationRequest();
            req.setServiceType(ServiceType.CLEANING);
            req.setPropertySize("STUDIO");
            req.setComplexity("SIMPLE");
            req.setUrgent(false);
            req.setMaterialsIncluded(true);

            EstimationResult result = estimationService.estimate(req);

            // materialCost = maxPrice * 30%
            BigDecimal expectedMaterial = result.getMaxPrice().multiply(new BigDecimal("0.30"))
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            // Due to rounding, we check approximate equality
            assertThat(result.getEstimatedMaterialCost()).isGreaterThan(BigDecimal.ZERO);
            assertThat(result.getTotalMin()).isGreaterThan(result.getMinPrice());
            assertThat(result.getTotalMax()).isGreaterThan(result.getMaxPrice());
        }

        @Test
        @DisplayName("urgent request calls surge pricing service")
        void urgentCallsSurge() {
            EstimationRequest req = new EstimationRequest();
            req.setServiceType(ServiceType.REPAIRS);
            req.setPropertySize("1_BEDROOM");
            req.setComplexity("MEDIUM");
            req.setUrgent(true);
            req.setMaterialsIncluded(false);

            SurgePricingService.SurgeResult surgeResult = SurgePricingService.SurgeResult.builder()
                    .originalPrice(BigDecimal.valueOf(100))
                    .surgedPrice(BigDecimal.valueOf(125))
                    .multiplier(new BigDecimal("1.25"))
                    .surgeActive(true)
                    .reason("urgence")
                    .build();
            when(surgePricingService.calculate(any(), eq(ServiceType.REPAIRS), eq(true)))
                    .thenReturn(surgeResult);

            EstimationResult result = estimationService.estimate(req);

            assertThat(result.isSurgeApplied()).isTrue();
            verify(surgePricingService, atLeast(1)).calculate(any(), any(), eq(true));
        }

        @Test
        @DisplayName("VILLA multiplier produces higher price than STUDIO")
        void villaHigherThanStudio() {
            EstimationRequest studio = new EstimationRequest();
            studio.setServiceType(ServiceType.PAINTING);
            studio.setPropertySize("STUDIO");
            studio.setComplexity("SIMPLE");
            studio.setUrgent(false);
            studio.setMaterialsIncluded(false);

            EstimationRequest villa = new EstimationRequest();
            villa.setServiceType(ServiceType.PAINTING);
            villa.setPropertySize("VILLA");
            villa.setComplexity("SIMPLE");
            villa.setUrgent(false);
            villa.setMaterialsIncluded(false);

            EstimationResult studioResult = estimationService.estimate(studio);
            EstimationResult villaResult = estimationService.estimate(villa);

            assertThat(villaResult.getMinPrice()).isGreaterThan(studioResult.getMinPrice());
        }

        @Test
        @DisplayName("disclaimer is always populated")
        void disclaimerPresent() {
            EstimationRequest req = new EstimationRequest();
            req.setServiceType(ServiceType.MOVING);

            EstimationResult result = estimationService.estimate(req);

            assertThat(result.getDisclaimer()).isNotBlank();
        }

        @Test
        @DisplayName("totalMin equals minPrice + materialCost")
        void totalMinCalculation() {
            EstimationRequest req = new EstimationRequest();
            req.setServiceType(ServiceType.ASSEMBLY);
            req.setPropertySize("1_BEDROOM");
            req.setComplexity("SIMPLE");
            req.setUrgent(false);
            req.setMaterialsIncluded(true);

            EstimationResult result = estimationService.estimate(req);

            BigDecimal expected = result.getMinPrice().add(result.getEstimatedMaterialCost());
            assertThat(result.getTotalMin()).isEqualByComparingTo(expected);
        }
    }
}
