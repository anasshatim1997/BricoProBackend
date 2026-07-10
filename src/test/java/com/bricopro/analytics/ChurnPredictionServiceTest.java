package com.bricopro.analytics;

import com.bricopro.analytics.service.ChurnPredictionService;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChurnPredictionService")
class ChurnPredictionServiceTest {

    @Mock TaskRepository taskRepository;

    @InjectMocks ChurnPredictionService churnService;

    private Page<Object> pageOfSize(int size) {
        return new PageImpl<>(java.util.Collections.nCopies(size, new Object()));
    }

    @Test
    @DisplayName("flags HIGH risk when there's no recent activity — triggers both NO_RECENT_ACTIVITY and LOW_ENGAGEMENT since 0 < 2")
    void flagsHighRiskForNoRecentActivity() {
        when(taskRepository.findByClientIdAndStatus(eq(3L), eq(TaskStatus.COMPLETED), any(Pageable.class)))
                .thenReturn((Page) pageOfSize(0));
        when(taskRepository.findByClientIdAndStatus(eq(3L), eq(TaskStatus.CANCELLED), any(Pageable.class)))
                .thenReturn((Page) pageOfSize(0));

        ChurnPredictionService.ChurnReport report = churnService.predictClientChurn(3L);

        assertThat(report.getChurnRisk()).isEqualTo("HIGH");
        assertThat(report.getSignals()).contains("NO_RECENT_ACTIVITY", "LOW_ENGAGEMENT");
        assertThat(report.getChurnScore()).isEqualTo(60.0);
    }

    @Test
    @DisplayName("combines high-cancellation and low-engagement signals into a HIGH score")
    void combinesMultipleSignals() {
        when(taskRepository.findByClientIdAndStatus(eq(3L), eq(TaskStatus.COMPLETED), any(Pageable.class)))
                .thenReturn((Page) pageOfSize(1));
        when(taskRepository.findByClientIdAndStatus(eq(3L), eq(TaskStatus.CANCELLED), any(Pageable.class)))
                .thenReturn((Page) pageOfSize(5));

        ChurnPredictionService.ChurnReport report = churnService.predictClientChurn(3L);

        assertThat(report.getSignals()).contains("HIGH_CANCELLATIONS", "LOW_ENGAGEMENT");
        assertThat(report.getChurnRisk()).isEqualTo("HIGH");
    }

    @Test
    @DisplayName("returns LOW risk with no signals for an engaged, low-cancellation client")
    void returnsLowRiskForEngagedClient() {
        when(taskRepository.findByClientIdAndStatus(eq(3L), eq(TaskStatus.COMPLETED), any(Pageable.class)))
                .thenReturn((Page) pageOfSize(5));
        when(taskRepository.findByClientIdAndStatus(eq(3L), eq(TaskStatus.CANCELLED), any(Pageable.class)))
                .thenReturn((Page) pageOfSize(0));

        ChurnPredictionService.ChurnReport report = churnService.predictClientChurn(3L);

        assertThat(report.getChurnRisk()).isEqualTo("LOW");
        assertThat(report.getSignals()).isEmpty();
        assertThat(report.getChurnScore()).isZero();
    }

    @Test
    @DisplayName("a single LOW_ENGAGEMENT signal alone (score 20) still resolves to LOW risk")
    void singleLowEngagementSignalStaysLowRisk() {
        when(taskRepository.findByClientIdAndStatus(eq(3L), eq(TaskStatus.COMPLETED), any(Pageable.class)))
                .thenReturn((Page) pageOfSize(1));
        when(taskRepository.findByClientIdAndStatus(eq(3L), eq(TaskStatus.CANCELLED), any(Pageable.class)))
                .thenReturn((Page) pageOfSize(0));

        ChurnPredictionService.ChurnReport report = churnService.predictClientChurn(3L);

        assertThat(report.getSignals()).containsExactly("LOW_ENGAGEMENT");
        assertThat(report.getChurnScore()).isEqualTo(20.0);
        assertThat(report.getChurnRisk()).isEqualTo("LOW");
    }

    @Test
    @DisplayName("HIGH_CANCELLATIONS alone (score 30) resolves to MEDIUM risk")
    void highCancellationsAloneIsMediumRisk() {
        when(taskRepository.findByClientIdAndStatus(eq(3L), eq(TaskStatus.COMPLETED), any(Pageable.class)))
                .thenReturn((Page) pageOfSize(3));
        when(taskRepository.findByClientIdAndStatus(eq(3L), eq(TaskStatus.CANCELLED), any(Pageable.class)))
                .thenReturn((Page) pageOfSize(4));

        ChurnPredictionService.ChurnReport report = churnService.predictClientChurn(3L);

        assertThat(report.getSignals()).containsExactly("HIGH_CANCELLATIONS");
        assertThat(report.getChurnScore()).isEqualTo(30.0);
        assertThat(report.getChurnRisk()).isEqualTo("MEDIUM");
    }
}
