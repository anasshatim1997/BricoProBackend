package com.bricopro.analytics.service;

import com.bricopro.task.entity.Task.TaskStatus;
import com.bricopro.task.repository.TaskRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private final TaskRepository taskRepository;

    public FraudReport analyzeWorker(Long workerId) {
        long cancelled = taskRepository.findByWorkerIdAndStatus(workerId, TaskStatus.CANCELLED,
                PageRequest.of(0, Integer.MAX_VALUE)).getTotalElements();
        long disputed  = taskRepository.findByWorkerIdAndStatus(workerId, TaskStatus.DISPUTED,
                PageRequest.of(0, Integer.MAX_VALUE)).getTotalElements();
        long total     = taskRepository.countByWorkerId(workerId);

        double cancellationRate = total > 0 ? (double) cancelled / total * 100 : 0;
        double disputeRate      = total > 0 ? (double) disputed  / total * 100 : 0;

        List<String> flags = new ArrayList<>();
        String riskLevel = "LOW";

        if (cancellationRate > 30) { flags.add("HIGH_CANCELLATION_RATE"); riskLevel = "HIGH"; }
        if (disputeRate > 20)      { flags.add("HIGH_DISPUTE_RATE");       riskLevel = "HIGH"; }
        if (cancelled > 5)         { flags.add("MULTIPLE_CANCELLATIONS");  riskLevel = "MEDIUM"; }
        if (disputed > 2)          { flags.add("MULTIPLE_DISPUTES");       riskLevel = "MEDIUM"; }

        return new FraudReport(workerId, riskLevel, flags, cancellationRate, disputeRate);
    }

    @Data @AllArgsConstructor
    public static class FraudReport {
        private Long workerId;
        private String riskLevel;
        private List<String> flags;
        private double cancellationRate;
        private double disputeRate;
    }
}
