package com.bricopro.task;

import com.bricopro.task.scheduler.ReliabilityScheduler;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.WorkerProfile;
import com.bricopro.user.repository.UserRepository;
import com.bricopro.user.repository.WorkerProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReliabilityScheduler")
class ReliabilitySchedulerTest {

    @Mock UserRepository userRepository;
    @Mock WorkerProfileRepository workerProfileRepository;

    @InjectMocks ReliabilityScheduler scheduler;

    @Test
    @DisplayName("resets cancellationCountThisMonth to 0 for every user")
    void resetsUserCounters() {
        User u1 = User.builder().id(1L).cancellationCountThisMonth(3).build();
        User u2 = User.builder().id(2L).cancellationCountThisMonth(5).build();
        when(userRepository.findAll()).thenReturn(List.of(u1, u2));
        when(workerProfileRepository.findAll()).thenReturn(List.of());

        scheduler.resetMonthlyCounters();

        assertThat(u1.getCancellationCountThisMonth()).isZero();
        assertThat(u2.getCancellationCountThisMonth()).isZero();
        verify(userRepository).saveAll(List.of(u1, u2));
    }

    @Test
    @DisplayName("resets cancellationCountThisMonth to 0 for every worker profile")
    void resetsWorkerProfileCounters() {
        WorkerProfile wp1 = WorkerProfile.builder().id(1L).cancellationCountThisMonth(2).build();
        when(userRepository.findAll()).thenReturn(List.of());
        when(workerProfileRepository.findAll()).thenReturn(List.of(wp1));

        scheduler.resetMonthlyCounters();

        assertThat(wp1.getCancellationCountThisMonth()).isZero();
        verify(workerProfileRepository).saveAll(List.of(wp1));
    }

    @Test
    @DisplayName("does nothing harmful when there are no users or profiles at all")
    void handlesEmptyDatasetGracefully() {
        when(userRepository.findAll()).thenReturn(List.of());
        when(workerProfileRepository.findAll()).thenReturn(List.of());

        assertThatCode(() -> scheduler.resetMonthlyCounters()).doesNotThrowAnyException();

        verify(userRepository).saveAll(List.of());
        verify(workerProfileRepository).saveAll(List.of());
    }
}
