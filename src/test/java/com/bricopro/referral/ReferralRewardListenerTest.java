package com.bricopro.referral;

import com.bricopro.referral.service.ReferralRewardListener;
import com.bricopro.referral.service.ReferralService;
import com.bricopro.task.entity.Task;
import com.bricopro.task.entity.Task.TaskStatus;
import com.bricopro.task.repository.TaskRepository;
import com.bricopro.task.service.TaskService.TaskStatusChangedEvent;
import com.bricopro.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReferralRewardListener")
class ReferralRewardListenerTest {

    @Mock TaskRepository  taskRepository;
    @Mock ReferralService referralService;

    @InjectMocks ReferralRewardListener listener;

    private User client;
    private Task task;

    @BeforeEach
    void setup() {
        client = User.builder().id(2L).firstName("Yassine").build();
        task = Task.builder().id(10L).client(client).status(TaskStatus.COMPLETED).build();
    }

    @Test
    @DisplayName("credits the reward when this is the client's first completed task")
    void creditsOnFirstCompletedTask() {
        when(taskRepository.countByClientIdAndStatus(2L, TaskStatus.COMPLETED)).thenReturn(1L);

        listener.onTaskStatusChanged(new TaskStatusChangedEvent(task));

        verify(referralService).creditPendingRewardIfFirstCompletedTask(2L);
    }

    @Test
    @DisplayName("does not credit again on the client's second completed task")
    void doesNotCreditOnSecondCompletedTask() {
        when(taskRepository.countByClientIdAndStatus(2L, TaskStatus.COMPLETED)).thenReturn(2L);

        listener.onTaskStatusChanged(new TaskStatusChangedEvent(task));

        verify(referralService, never()).creditPendingRewardIfFirstCompletedTask(anyLong());
    }

    @Test
    @DisplayName("ignores status changes that aren't COMPLETED")
    void ignoresNonCompletedStatus() {
        Task startedTask = Task.builder().id(11L).client(client).status(TaskStatus.STARTED).build();

        listener.onTaskStatusChanged(new TaskStatusChangedEvent(startedTask));

        verifyNoInteractions(taskRepository);
        verifyNoInteractions(referralService);
    }

    @Test
    @DisplayName("ignores tasks with no assigned client")
    void ignoresTaskWithNoClient() {
        Task noClientTask = Task.builder().id(12L).client(null).status(TaskStatus.COMPLETED).build();

        listener.onTaskStatusChanged(new TaskStatusChangedEvent(noClientTask));

        verifyNoInteractions(taskRepository);
        verifyNoInteractions(referralService);
    }
}
