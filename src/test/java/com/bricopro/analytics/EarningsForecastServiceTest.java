package com.bricopro.analytics;

import com.bricopro.analytics.service.EarningsForecastService;
import com.bricopro.payment.repository.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EarningsForecastService")
class EarningsForecastServiceTest {

    @Mock PaymentRepository paymentRepository;

    @InjectMocks EarningsForecastService forecastService;

    @Test
    @DisplayName("computes a monthly average and a 12x annual projection")
    void computesAverageAndProjection() {
        when(paymentRepository.sumWorkerRevenueByMonth(eq(9L), anyInt(), anyInt()))
                .thenReturn(new BigDecimal("1000"));

        var report = forecastService.forecast(9L);

        int monthsSoFar = LocalDate.now().getMonthValue();
        assertThat(report.getMonthlyAverage()).isEqualByComparingTo("1000.00");
        assertThat(report.getAnnualProjection()).isEqualByComparingTo("12000.00");
        assertThat(report.getHistoricalByMonth()).hasSize(monthsSoFar);
    }

    @Test
    @DisplayName("treats a null monthly revenue as zero rather than throwing")
    void treatsNullRevenueAsZero() {
        when(paymentRepository.sumWorkerRevenueByMonth(eq(9L), anyInt(), anyInt())).thenReturn(null);

        var report = forecastService.forecast(9L);

        assertThat(report.getMonthlyAverage()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(report.getAnnualProjection()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("projects next month slightly above the average (5% growth assumption)")
    void nextMonthIsAboveAverage() {
        when(paymentRepository.sumWorkerRevenueByMonth(eq(9L), anyInt(), anyInt()))
                .thenReturn(new BigDecimal("1000"));

        var report = forecastService.forecast(9L);

        assertThat(report.getNextMonthForecast()).isGreaterThan(report.getMonthlyAverage());
    }
}
