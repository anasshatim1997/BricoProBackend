package com.bricopro.task;

import com.bricopro.task.scheduler.RatingSuspensionScheduler;
import com.bricopro.task.service.RatingSuspensionService;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.User.Status;
import com.bricopro.user.entity.WorkerProfile;
import com.bricopro.user.repository.WorkerProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RatingSuspensionScheduler")
class RatingSuspensionSchedulerTest {

    @Mock WorkerProfileRepository workerProfileRepository;
    @Mock RatingSuspensionService ratingSuspensionService;

    @InjectMocks RatingSuspensionScheduler scheduler;

    @Test
    @DisplayName("evaluates every active worker on a single page")
    void evaluatesSinglePage() {
        WorkerProfile wp1 = WorkerProfile.builder().user(User.builder().id(1L).build()).build();
        WorkerProfile wp2 = WorkerProfile.builder().user(User.builder().id(2L).build()).build();

        Page<WorkerProfile> onlyPage = new PageImpl<>(List.of(wp1, wp2), PageRequest.of(0, 200), 2);
        when(workerProfileRepository.findByUserStatus(eq(Status.ACTIVE), any(Pageable.class)))
                .thenReturn(onlyPage);

        scheduler.sweepActiveWorkers();

        verify(ratingSuspensionService).evaluateWorkerRating(1L);
        verify(ratingSuspensionService).evaluateWorkerRating(2L);
        verify(workerProfileRepository, times(1)).findByUserStatus(eq(Status.ACTIVE), any(Pageable.class));
    }

    @Test
    @DisplayName("REGRESSION: walks every page until hasNext() is false, not just the first page")
    void walksMultiplePages() {
        WorkerProfile wpPage0 = WorkerProfile.builder().user(User.builder().id(10L).build()).build();
        WorkerProfile wpPage1 = WorkerProfile.builder().user(User.builder().id(20L).build()).build();

        Page<WorkerProfile> page0 = new PageImpl<>(List.of(wpPage0), PageRequest.of(0, 1), 2);
        Page<WorkerProfile> page1 = new PageImpl<>(List.of(wpPage1), PageRequest.of(1, 1), 2);

        when(workerProfileRepository.findByUserStatus(eq(Status.ACTIVE), eq(PageRequest.of(0, 200))))
                .thenReturn(page0);
        when(workerProfileRepository.findByUserStatus(eq(Status.ACTIVE), eq(PageRequest.of(1, 200))))
                .thenReturn(page1);

        scheduler.sweepActiveWorkers();

        verify(ratingSuspensionService).evaluateWorkerRating(10L);
        verify(ratingSuspensionService).evaluateWorkerRating(20L);
        verify(workerProfileRepository, times(2)).findByUserStatus(eq(Status.ACTIVE), any(Pageable.class));
    }

    @Test
    @DisplayName("does nothing when there are no active workers at all")
    void noActiveWorkersDoesNothing() {
        Page<WorkerProfile> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 200), 0);
        when(workerProfileRepository.findByUserStatus(eq(Status.ACTIVE), any(Pageable.class)))
                .thenReturn(emptyPage);

        scheduler.sweepActiveWorkers();

        verifyNoInteractions(ratingSuspensionService);
    }
}
