package com.bricopro.analytics.service;

import com.bricopro.payment.repository.PaymentRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class EarningsForecastService {

    private final PaymentRepository paymentRepository;

    public ForecastReport forecast(Long workerId) {
        int year = LocalDate.now().getYear();
        List<BigDecimal> monthlyRevenues = IntStream.rangeClosed(1, LocalDate.now().getMonthValue())
                .mapToObj(m -> {
                    BigDecimal r = paymentRepository.sumWorkerRevenueByMonth(workerId, m, year);
                    return r != null ? r : BigDecimal.ZERO;
                })
                .collect(Collectors.toList());

        BigDecimal avg = monthlyRevenues.isEmpty() ? BigDecimal.ZERO
                : monthlyRevenues.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(monthlyRevenues.size()), 2, RoundingMode.HALF_UP);

        BigDecimal projected = avg.multiply(BigDecimal.valueOf(12));
        BigDecimal nextMonth = avg.multiply(new BigDecimal("1.05"));

        Map<String, BigDecimal> monthly = new LinkedHashMap<>();
        for (int m = 1; m <= monthlyRevenues.size(); m++) {
            monthly.put(String.format("%02d/%d", m, year), monthlyRevenues.get(m - 1));
        }

        return new ForecastReport(workerId, avg, nextMonth, projected, monthly);
    }

    @Data @AllArgsConstructor
    public static class ForecastReport {
        private Long workerId;
        private BigDecimal monthlyAverage;
        private BigDecimal nextMonthForecast;
        private BigDecimal annualProjection;
        private Map<String, BigDecimal> historicalByMonth;
    }
}
