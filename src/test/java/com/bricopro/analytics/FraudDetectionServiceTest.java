package com.bricopro.analytics;

import com.bricopro.analytics.service.FraudDetectionService;
import com.bricopro.task.entity.Task.TaskStatus;
import com.bricopro.task.repository.TaskRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FraudDetectionService")
class FraudDetectionServiceTest {

    @Mock TaskRepository taskRepository;

    @InjectMocks FraudDetectionService fraudService;

    private Page<Object> pageOfSize(int size) {
        return new PageImpl<>(java.util.Collections.nCopies(size, new Object()));
    }

    @Test
    @DisplayName("flags HIGH risk when cancellation rate exceeds 30%")
    void flagsHighRiskForHighCancellationRate() {
        when(taskRepository.findByWorkerIdAndStatus(eq(2L), eq(TaskStatus.CANCELLED), any(Pageable.class)))
                .thenReturn((Page) pageOfSize(4));
        when(taskRepository.findByWorkerIdAndStatus(eq(2L), eq(TaskStatus.DISPUTED), any(Pageable.class)))
                .thenReturn((Page) pageOfSize(0));
        when(taskRepository.countByWorkerId(2L)).thenReturn(10L);

        FraudDetectionService.FraudReport report = fraudService.analyzeWorker(2L);

        assertThat(report.getRiskLevel()).isEqualTo("HIGH");
        assertThat(report.getFlags()).contains("HIGH_CANCELLATION_RATE");
        assertThat(report.getCancellationRate()).isEqualTo(40.0);
    }

    @Test
    @DisplayName("flags HIGH risk when dispute rate exceeds 20%")
    void flagsHighRiskForHighDisputeRate() {
        when(taskRepository.findByWorkerIdAndStatus(eq(2L), eq(TaskStatus.CANCELLED), any(Pageable.class)))
                .thenReturn((Page) pageOfSize(0));
        when(taskRepository.findByWorkerIdAndStatus(eq(2L), eq(TaskStatus.DISPUTED), any(Pageable.class)))
                .thenReturn((Page) pageOfSize(3));
        when(taskRepository.countByWorkerId(2L)).thenReturn(10L);

        FraudDetectionService.FraudReport report = fraudService.analyzeWorker(2L);

        assertThat(report.getRiskLevel()).isEqualTo("HIGH");
        assertThat(report.getFlags()).contains("HIGH_DISPUTE_RATE");
    }

    @Test
    @DisplayName("flags MEDIUM risk for repeat cancellations under the HIGH threshold")
    void flagsMediumRiskForRepeatCancellations() {
        when(taskRepository.findByWorkerIdAndStatus(eq(2L), eq(TaskStatus.CANCELLED), any(Pageable.class)))
                .thenReturn((Page) pageOfSize(6));
        when(taskRepository.findByWorkerIdAndStatus(eq(2L), eq(TaskStatus.DISPUTED), any(Pageable.class)))
                .thenReturn((Page) pageOfSize(0));
        when(taskRepository.countByWorkerId(2L)).thenReturn(100L);

        FraudDetectionService.FraudReport report = fraudService.analyzeWorker(2L);

        assertThat(report.getFlags()).contains("MULTIPLE_CANCELLATIONS");
    }

    @Test
    @DisplayName("returns LOW risk with no flags for a clean record")
    void returnsLowRiskForCleanRecord() {
        when(taskRepository.findByWorkerIdAndStatus(eq(2L), eq(TaskStatus.CANCELLED), any(Pageable.class)))
                .thenReturn((Page) pageOfSize(0));
        when(taskRepository.findByWorkerIdAndStatus(eq(2L), eq(TaskStatus.DISPUTED), any(Pageable.class)))
                .thenReturn((Page) pageOfSize(0));
        when(taskRepository.countByWorkerId(2L)).thenReturn(50L);

        FraudDetectionService.FraudReport report = fraudService.analyzeWorker(2L);

        assertThat(report.getRiskLevel()).isEqualTo("LOW");
        assertThat(report.getFlags()).isEmpty();
    }

    @Test
    @DisplayName("does not divide by zero when the worker has no tasks at all")
    void handlesZeroTasksGracefully() {
        when(taskRepository.findByWorkerIdAndStatus(eq(2L), any(), any(Pageable.class)))
                .thenReturn((Page) pageOfSize(0));
        when(taskRepository.countByWorkerId(2L)).thenReturn(0L);

        FraudDetectionService.FraudReport report = fraudService.analyzeWorker(2L);

        assertThat(report.getCancellationRate()).isZero();
        assertThat(report.getDisputeRate()).isZero();
        assertThat(report.getRiskLevel()).isEqualTo("LOW");
    }
}
