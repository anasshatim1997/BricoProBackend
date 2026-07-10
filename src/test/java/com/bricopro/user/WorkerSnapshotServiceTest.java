package com.bricopro.user;

import com.bricopro.user.entity.WorkerProfile;
import com.bricopro.user.entity.WorkerSnapshotHistory;
import com.bricopro.user.repository.WorkerProfileRepository;
import com.bricopro.user.repository.WorkerSnapshotHistoryRepository;
import com.bricopro.user.service.WorkerSnapshotService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkerSnapshotService")
class WorkerSnapshotServiceTest {

    @Mock WorkerProfileRepository workerProfileRepository;
    @Mock WorkerSnapshotHistoryRepository snapshotRepository;

    @InjectMocks WorkerSnapshotService snapshotService;

    @Test
    @DisplayName("captures the six scoring fields at the exact moment of assignment")
    void capturesScoringFieldsAtAssignment() {
        WorkerProfile profile = WorkerProfile.builder()
                .averageRating(new BigDecimal("4.80"))
                .totalReviews(40)
                .totalMissions(80)
                .responseRate(new BigDecimal("95.00"))
                .cancellationCount(1)
                .isPremium(true)
                .build();
        when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(profile));

        snapshotService.captureOnAssignment(2L, 17L);

        verify(snapshotRepository).save(argThat(s ->
                s.getWorkerId().equals(2L)
                        && s.getTaskId().equals(17L)
                        && s.getAverageRating().compareTo(new BigDecimal("4.80")) == 0
                        && s.getTotalReviews() == 40
                        && s.getTotalMissions() == 80
                        && s.getResponseRate().compareTo(new BigDecimal("95.00")) == 0
                        && s.getCancellationCount() == 1
                        && s.isPremium()));
    }

    @Test
    @DisplayName("skips silently when the worker has no profile yet, rather than throwing")
    void skipsWhenNoProfile() {
        when(workerProfileRepository.findByUserId(999L)).thenReturn(Optional.empty());

        assertThatCode(() -> snapshotService.captureOnAssignment(999L, 17L))
                .doesNotThrowAnyException();

        verify(snapshotRepository, never()).save(any());
    }
}
