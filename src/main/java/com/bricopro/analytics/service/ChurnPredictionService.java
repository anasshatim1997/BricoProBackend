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
public class ChurnPredictionService {

    private final TaskRepository taskRepository;

    public ChurnReport predictClientChurn(Long clientId) {
        long recentTasks = taskRepository.findByClientIdAndStatus(clientId, TaskStatus.COMPLETED,
                PageRequest.of(0, 5)).getTotalElements();
        long cancelledTasks = taskRepository.findByClientIdAndStatus(clientId, TaskStatus.CANCELLED,
                PageRequest.of(0, Integer.MAX_VALUE)).getTotalElements();

        double churnScore = 0;
        List<String> signals = new ArrayList<>();

        if (recentTasks == 0)     { churnScore += 40; signals.add("NO_RECENT_ACTIVITY"); }
        if (cancelledTasks > 3)   { churnScore += 30; signals.add("HIGH_CANCELLATIONS"); }
        if (recentTasks < 2)      { churnScore += 20; signals.add("LOW_ENGAGEMENT"); }

        String risk = churnScore >= 50 ? "HIGH" : churnScore >= 30 ? "MEDIUM" : "LOW";

        return new ChurnReport(clientId, risk, churnScore, signals);
    }

    @Data @AllArgsConstructor
    public static class ChurnReport {
        private Long clientId;
        private String churnRisk;
        private double churnScore;
        private List<String> signals;
    }
}