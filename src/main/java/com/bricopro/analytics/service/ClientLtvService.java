package com.bricopro.analytics.service;

import com.bricopro.payment.repository.PaymentRepository;
import com.bricopro.task.entity.Task.TaskStatus;
import com.bricopro.task.repository.TaskRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class ClientLtvService {

    private final TaskRepository    taskRepository;
    private final PaymentRepository paymentRepository;

    public ClientLtvReport getClientLtv(Long clientId) {
        long total       = taskRepository.countByClientId(clientId);
        long completed   = taskRepository.findByClientIdAndStatus(clientId, TaskStatus.COMPLETED,
                PageRequest.of(0, Integer.MAX_VALUE)).getTotalElements();
        BigDecimal spent = paymentRepository.sumAllClientSpent(clientId);

        BigDecimal avgPerTask = (completed > 0 && spent != null)
                ? spent.divide(BigDecimal.valueOf(completed), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        String segment = spent != null && spent.compareTo(new BigDecimal("5000")) > 0 ? "VIP"
                       : spent != null && spent.compareTo(new BigDecimal("1000")) > 0 ? "REGULAR"
                       : "NEW";

        return new ClientLtvReport(clientId, total, completed, spent != null ? spent : BigDecimal.ZERO,
                avgPerTask, segment);
    }

    @Data @AllArgsConstructor
    public static class ClientLtvReport {
        private Long clientId;
        private long totalRequests;
        private long completedRequests;
        private BigDecimal totalSpent;
        private BigDecimal avgSpentPerTask;
        private String segment;
    }
}
