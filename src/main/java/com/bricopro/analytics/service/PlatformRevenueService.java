package com.bricopro.analytics.service;

import com.bricopro.payment.repository.PaymentRepository;
import com.bricopro.task.repository.TaskRepository;
import com.bricopro.user.entity.User.Role;
import com.bricopro.user.entity.User.Status;
import com.bricopro.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlatformRevenueService {

    private final PaymentRepository paymentRepository;
    private final UserRepository    userRepository;
    private final TaskRepository    taskRepository;

    public PlatformRevenueReport getReport(int year) {
        Map<String, BigDecimal> monthly = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;

        for (int m = 1; m <= 12; m++) {
            BigDecimal rev = paymentRepository.sumPlatformFeeByMonthAndYear(m, year);
            BigDecimal monthRev = rev != null ? rev : BigDecimal.ZERO;
            monthly.put(String.format("%02d/%d", m, year), monthRev);
            total = total.add(monthRev);
        }

        long totalWorkers = userRepository.search(Role.WORKER, Status.ACTIVE, null,
                PageRequest.of(0, 1)).getTotalElements();
        long totalClients = userRepository.search(Role.CLIENT, Status.ACTIVE, null,
                PageRequest.of(0, 1)).getTotalElements();
        long totalTasks   = taskRepository.count();

        return new PlatformRevenueReport(year, total, monthly, totalWorkers, totalClients, totalTasks);
    }

    @Data @AllArgsConstructor
    public static class PlatformRevenueReport {
        private int year;
        private BigDecimal totalRevenue;
        private Map<String, BigDecimal> revenueByMonth;
        private long activeWorkers;
        private long activeClients;
        private long totalTasks;
    }
}
