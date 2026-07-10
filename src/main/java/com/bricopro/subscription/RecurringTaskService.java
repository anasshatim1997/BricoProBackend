package com.bricopro.subscription;

import com.bricopro.task.entity.Task;
import com.bricopro.task.repository.TaskRepository;
import com.bricopro.user.entity.User;
import com.bricopro.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecurringTaskService {

    @Schema(description = "Recurring Repo", example = "value")
    private final RecurringTaskRepository recurringRepo;
    @Schema(description = "Task Repository", example = "value")
    private final TaskRepository          taskRepository;
    @Schema(description = "User Repository", example = "value")
    private final UserRepository          userRepository;

    @Transactional
    /**
     * Create.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public RecurringTask create(Long clientId, CreateRecurringRequest req) {
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        RecurringTask rt = RecurringTask.builder()
                .client(client)
                .serviceType(req.getServiceType())
                .title(req.getTitle())
                .description(req.getDescription())
                .address(req.getAddress())
                .frequency(req.getFrequency())
                .preferredTime(req.getPreferredTime())
                .nextScheduledDate(req.getStartDate())
                .endDate(req.getEndDate())
                .build();

        return recurringRepo.save(rt);
    }

    /**
     * Get For Client.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public Page<RecurringTask> getForClient(Long clientId, Pageable pageable) {
        return recurringRepo.findByClientId(clientId, pageable);
    }

    @Transactional
    /**
     * Pause.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public RecurringTask pause(Long id, Long clientId) {
        RecurringTask rt = findAndValidate(id, clientId);
        rt.setStatus(RecurringTask.RecurringStatus.PAUSED);
        return recurringRepo.save(rt);
    }

    @Transactional
    /**
     * Cancel.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public RecurringTask cancel(Long id, Long clientId) {
        RecurringTask rt = findAndValidate(id, clientId);
        rt.setStatus(RecurringTask.RecurringStatus.CANCELLED);
        return recurringRepo.save(rt);
    }

@Scheduled(cron = "0 0 6 * * *")
    @Transactional
    /**
     * Process Recurring Tasks.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public void processRecurringTasks() {
        LocalDate today = LocalDate.now();
        List<RecurringTask> due = recurringRepo
                .findByStatusAndNextScheduledDateLessThanEqual(RecurringTask.RecurringStatus.ACTIVE, today);

        for (RecurringTask rt : due) {
            if (rt.getEndDate() != null && today.isAfter(rt.getEndDate())) {
                rt.setStatus(RecurringTask.RecurringStatus.CANCELLED);
                recurringRepo.save(rt);
                continue;
            }

            Task task = Task.builder()
                    .client(rt.getClient())
                    .worker(rt.getPreferredWorker())
                    .serviceType(rt.getServiceType())
                    .title(rt.getTitle() + " (récurrent)")
                    .description(rt.getDescription())
                    .address(rt.getAddress())
                    .scheduledDate(rt.getNextScheduledDate())
                    .scheduledStart(rt.getPreferredTime() != null ? rt.getPreferredTime() : LocalTime.of(9, 0))
                    .status(rt.getPreferredWorker() != null ? Task.TaskStatus.CONFIRMED : Task.TaskStatus.SEARCHING)
                    .build();
            taskRepository.save(task);

            rt.setNextScheduledDate(nextDate(rt.getFrequency(), rt.getNextScheduledDate()));
            recurringRepo.save(rt);
        }
    }

    private LocalDate nextDate(RecurringTask.Frequency freq, LocalDate from) {
        return switch (freq) {
            case DAILY     -> from.plusDays(1);
            case WEEKLY    -> from.plusWeeks(1);
            case BIWEEKLY  -> from.plusWeeks(2);
            case MONTHLY   -> from.plusMonths(1);
        };
    }

    private RecurringTask findAndValidate(Long id, Long clientId) {
        RecurringTask rt = recurringRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Recurring task not found"));
        if (!rt.getClient().getId().equals(clientId))
            throw new SecurityException("Not your recurring task");
        return rt;
    }
}
