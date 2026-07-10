package com.bricopro.referral.service;

import com.bricopro.task.entity.Task.TaskStatus;
import com.bricopro.task.repository.TaskRepository;
import com.bricopro.task.service.TaskService.TaskStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ReferralRewardListener {

    private final TaskRepository   taskRepository;
    private final ReferralService  referralService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskStatusChanged(TaskStatusChangedEvent event) {
        var task = event.task();
        if (task.getStatus() != TaskStatus.COMPLETED || task.getClient() == null) {
            return;
        }

        long completedCount = taskRepository.countByClientIdAndStatus(
                task.getClient().getId(), TaskStatus.COMPLETED);

        if (completedCount == 1) {
            referralService.creditPendingRewardIfFirstCompletedTask(task.getClient().getId());
        }
    }
}
