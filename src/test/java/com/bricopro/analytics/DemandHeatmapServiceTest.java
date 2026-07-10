package com.bricopro.analytics;

import com.bricopro.analytics.service.DemandHeatmapService;
import com.bricopro.task.entity.Task.TaskStatus;
import com.bricopro.task.repository.TaskRepository;
import com.bricopro.user.entity.WorkerProfile.ServiceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DemandHeatmapService")
class DemandHeatmapServiceTest {

    @Mock TaskRepository taskRepository;

    @InjectMocks DemandHeatmapService heatmapService;

    @Test
    @DisplayName("REGRESSION: calls the grouped query exactly once, never findByStatus per service type")
    void callsGroupedQueryOnce() {
        when(taskRepository.countByStatusGroupedByServiceType(TaskStatus.SEARCHING))
                .thenReturn(List.of(
                        new Object[]{ServiceType.PLUMBING, 15L},
                        new Object[]{ServiceType.CLEANING, 3L}
                ));

        heatmapService.getHeatmap();

        verify(taskRepository, times(1)).countByStatusGroupedByServiceType(TaskStatus.SEARCHING);
        verify(taskRepository, never()).findByStatus(any());
    }

    @Test
    @DisplayName("returns all 9 service types, with zero count for types absent from the grouped result")
    void includesAllServiceTypesEvenWithZeroCount() {
        when(taskRepository.countByStatusGroupedByServiceType(TaskStatus.SEARCHING))
                .thenReturn(List.of(new Object[]{ServiceType.PLUMBING, 15L}));

        List<DemandHeatmapService.HeatmapPoint> result = heatmapService.getHeatmap();

        assertThat(result).hasSize(ServiceType.values().length);
        assertThat(result.stream().filter(p -> p.getServiceType().equals("CLEANING")).findFirst().get()
                .getRequestCount()).isZero();
    }

    @Test
    @DisplayName("classifies demand level correctly at each threshold")
    void classifiesDemandLevels() {
        when(taskRepository.countByStatusGroupedByServiceType(TaskStatus.SEARCHING))
                .thenReturn(List.of(
                        new Object[]{ServiceType.PLUMBING, 15L},
                        new Object[]{ServiceType.CLEANING, 7L},
                        new Object[]{ServiceType.PAINTING, 2L}
                ));

        List<DemandHeatmapService.HeatmapPoint> result = heatmapService.getHeatmap();

        assertThat(byType(result, "PLUMBING").getDemandLevel()).isEqualTo("HIGH");
        assertThat(byType(result, "CLEANING").getDemandLevel()).isEqualTo("MEDIUM");
        assertThat(byType(result, "PAINTING").getDemandLevel()).isEqualTo("LOW");
    }

    @Test
    @DisplayName("sorts results by request count descending")
    void sortsDescending() {
        when(taskRepository.countByStatusGroupedByServiceType(TaskStatus.SEARCHING))
                .thenReturn(List.of(
                        new Object[]{ServiceType.PLUMBING, 3L},
                        new Object[]{ServiceType.CLEANING, 15L}
                ));

        List<DemandHeatmapService.HeatmapPoint> result = heatmapService.getHeatmap();

        assertThat(result.get(0).getServiceType()).isEqualTo("CLEANING");
    }

    private DemandHeatmapService.HeatmapPoint byType(List<DemandHeatmapService.HeatmapPoint> list, String type) {
        return list.stream().filter(p -> p.getServiceType().equals(type)).findFirst().orElseThrow();
    }
}
