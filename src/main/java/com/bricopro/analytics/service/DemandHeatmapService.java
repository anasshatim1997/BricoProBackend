package com.bricopro.analytics.service;

import com.bricopro.task.entity.Task.TaskStatus;
import com.bricopro.task.repository.TaskRepository;
import com.bricopro.user.entity.WorkerProfile.ServiceType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DemandHeatmapService {

    private final TaskRepository taskRepository;

    public List<HeatmapPoint> getHeatmap() {
        Map<ServiceType, Long> counts = new HashMap<>();
        for (Object[] row : taskRepository.countByStatusGroupedByServiceType(TaskStatus.SEARCHING)) {
            counts.put((ServiceType) row[0], (Long) row[1]);
        }

        return java.util.Arrays.stream(ServiceType.values())
                .map(type -> {
                    long count = counts.getOrDefault(type, 0L);
                    return new HeatmapPoint(type.name(), count,
                            count > 10 ? "HIGH" : count > 5 ? "MEDIUM" : "LOW");
                })
                .sorted(Comparator.comparingLong(HeatmapPoint::getRequestCount).reversed())
                .collect(Collectors.toList());
    }

    @Data @AllArgsConstructor
    public static class HeatmapPoint {
        private String serviceType;
        private long requestCount;
        private String demandLevel;
    }
}
