package com.bricopro.task.service;

import com.bricopro.matching.RealTimeMatchingService;
import com.bricopro.notification.service.CommunicationService;
import com.bricopro.notification.service.NotificationService;
import com.bricopro.task.dto.TaskDtos.*;
import com.bricopro.task.entity.Review;
import com.bricopro.task.entity.Task;
import com.bricopro.task.entity.Task.TaskStatus;
import com.bricopro.task.mapper.TaskMapper;
import com.bricopro.task.repository.ReviewRepository;
import com.bricopro.task.repository.TaskRepository;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.WorkerProfile.ServiceType;
import com.bricopro.user.repository.WorkerProfileRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Tag(name = "Task Service", description = "Business logic for Task Service")
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private static final Map<TaskStatus, Set<TaskStatus>> ALLOWED_TRANSITIONS = Map.of(
            TaskStatus.SEARCHING,  EnumSet.of(TaskStatus.CONFIRMED, TaskStatus.CANCELLED),
            TaskStatus.CONFIRMED,  EnumSet.of(TaskStatus.STARTED,   TaskStatus.CANCELLED),
            TaskStatus.STARTED,    EnumSet.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED),
            TaskStatus.COMPLETED,  EnumSet.noneOf(TaskStatus.class),
            TaskStatus.CANCELLED,  EnumSet.noneOf(TaskStatus.class)
    );

    private final TaskRepository          taskRepository;
    private final ReviewRepository        reviewRepository;
    private final WorkerProfileRepository workerProfileRepository;
    private final RatingSuspensionService ratingSuspensionService;
    private final NotificationService     notificationService;
    private final CommunicationService    communicationService;
    private final TaskMapper              mapper;
    private final RealTimeMatchingService realTimeMatchingService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public TaskResponse create(User client, CreateTaskRequest req) {
        Task task = Task.builder()
                .client(client)
                .serviceType(req.getServiceType())
                .title(req.getTitle())
                .description(req.getDescription())
                .address(req.getAddress())
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .scheduledDate(req.getScheduledDate())
                .scheduledStart(req.getScheduledStart())
                .scheduledEnd(req.getScheduledEnd())
                .budgetMin(req.getBudgetMin())
                .budgetMax(req.getBudgetMax())
                .isUrgent(req.isUrgent())
                .biddingEnabled(req.getBiddingEnabled() != null ? req.getBiddingEnabled() : false)
                .biddingDeadline(req.getBiddingDeadline())
                .autoAssignEnabled(req.getAutoAssignEnabled() != null ? req.getAutoAssignEnabled() : false)
                .status(TaskStatus.SEARCHING)
                .build();

        try {
            task = taskRepository.save(task);
        } catch (Exception e) {
            log.error("Failed to save task: {}", e.getMessage(), e);
            throw new RuntimeException("Database error while saving task", e);
        }

        try {
            if (Boolean.TRUE.equals(task.getAutoAssignEnabled())) {
                List<Long> workerIds = realTimeMatchingService.findMatchingWorkers(task);
                if (!workerIds.isEmpty()) {
                    realTimeMatchingService.notifyWorkers(workerIds, task);
                    realTimeMatchingService.autoAssign(task, workerIds);
                    task = taskRepository.save(task);
                }
            } else {
                notificationService.notifyAvailableWorkers(task);
            }
        } catch (Exception e) {
            log.error("Failed to notify/matching workers for task {}: {}", task.getId(), e.getMessage(), e);
        }

        return mapper.toResponse(task);
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long taskId) {
        return mapper.toResponse(findOrThrow(taskId));
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> getClientTasks(Long clientId, TaskStatus status, Pageable pageable) {
        Page<Task> tasks = status != null
                ? taskRepository.findByClientIdAndStatus(clientId, status, pageable)
                : taskRepository.findByClientId(clientId, pageable);
        return tasks.map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> getWorkerTasks(Long workerId, TaskStatus status, Pageable pageable) {
        Page<Task> tasks = status != null
                ? taskRepository.findByWorkerIdAndStatus(workerId, status, pageable)
                : taskRepository.findByWorkerId(workerId, pageable);
        return tasks.map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> getAvailableTasks(ServiceType serviceType, Pageable pageable) {
        return taskRepository.findAvailableForWorker(serviceType, pageable).map(mapper::toResponse);
    }

    @Transactional
    public TaskResponse acceptTask(User worker, Long taskId) {
        int updated = taskRepository.claimTask(taskId, worker.getId(), TaskStatus.SEARCHING, TaskStatus.CONFIRMED);
        if (updated == 0) {
            throw new IllegalStateException("Task is no longer available");
        }

        Task task = findOrThrow(taskId);
        eventPublisher.publishEvent(new TaskAcceptedEvent(task, worker));
        return mapper.toResponse(task);
    }

    @Transactional
    public TaskResponse updateStatus(User actor, Long taskId, UpdateTaskStatusRequest req) {
        Task task = findOrThrow(taskId);
        validateStatusTransition(actor, task, req.getStatus());
        task.setStatus(req.getStatus());
        if (req.getAgreedPrice()        != null) task.setAgreedPrice(req.getAgreedPrice());
        if (req.getCancellationReason() != null) task.setCancellationReason(req.getCancellationReason());
        task = taskRepository.save(task);

        eventPublisher.publishEvent(new TaskStatusChangedEvent(task));
        return mapper.toResponse(task);
    }

    @Transactional
    public ReviewResponse submitReview(User reviewer, Long taskId, CreateReviewRequest req) {
        Task task = findOrThrow(taskId);
        if (task.getStatus() != TaskStatus.COMPLETED) {
            throw new IllegalStateException("Can only review completed tasks");
        }
        if (reviewRepository.findByTaskIdAndReviewerId(taskId, reviewer.getId()).isPresent()) {
            throw new IllegalStateException("Already reviewed this task");
        }

        boolean reviewerIsClient = reviewer.getId().equals(task.getClient().getId());
        if (reviewerIsClient && task.getWorker() == null) {
            throw new IllegalStateException("Cannot review a task with no assigned worker");
        }

        User reviewee = reviewerIsClient ? task.getWorker() : task.getClient();

        Review review = Review.builder()
                .task(task)
                .reviewer(reviewer)
                .reviewee(reviewee)
                .rating(req.getRating())
                .comment(req.getComment())
                .build();
        review = reviewRepository.save(review);
        if (reviewerIsClient) {
            recalculateWorkerStats(reviewee.getId());
            ratingSuspensionService.evaluateWorkerRating(reviewee.getId());
        }

        eventPublisher.publishEvent(new ReviewReceivedEvent(review, reviewer));
        return mapper.toReviewResponse(review);
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviews(Long userId, Pageable pageable) {
        return reviewRepository.findByRevieweeId(userId, pageable).map(mapper::toReviewResponse);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskAccepted(TaskAcceptedEvent event) {
        Task task   = event.task();
        User worker = event.worker();
        notificationService.notifyTaskAccepted(task);
        if (task.getClient().getEmail() != null) {
            communicationService.sendTaskConfirmationEmail(
                    task.getClient().getEmail(),
                    task.getClient().getFirstName(),
                    task.getTitle(),
                    worker.getFirstName() + " " + worker.getLastName());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStatusChanged(TaskStatusChangedEvent event) {
        Task task = event.task();
        notificationService.notifyStatusChange(task);
        switch (task.getStatus()) {
            case COMPLETED -> {
                if (task.getClient().getEmail() != null)
                    communicationService.sendTaskCompletedEmail(
                            task.getClient().getEmail(),
                            task.getClient().getFirstName(),
                            task.getTitle());
            }
            case CANCELLED -> {
                String cancelledBy = task.getCancelledBy() != null
                        ? task.getCancelledBy().name().toLowerCase() : "unknown";
                if (task.getClient().getEmail() != null)
                    communicationService.sendTaskCancelledEmail(
                            task.getClient().getEmail(),
                            task.getClient().getFirstName(),
                            task.getTitle(), cancelledBy);
                if (task.getWorker() != null && task.getWorker().getEmail() != null)
                    communicationService.sendTaskCancelledEmail(
                            task.getWorker().getEmail(),
                            task.getWorker().getFirstName(),
                            task.getTitle(), cancelledBy);
            }
            default -> {}
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReviewReceived(ReviewReceivedEvent event) {
        Review review   = event.review();
        User   reviewer = event.reviewer();
        notificationService.notifyReviewReceived(review);
        User reviewee = review.getReviewee();
        if (reviewee.getEmail() != null) {
            communicationService.sendReviewReceivedEmail(
                    reviewee.getEmail(),
                    reviewee.getFirstName(),
                    review.getRating(),
                    reviewer.getFirstName() + " " + reviewer.getLastName());
        }
    }

    private void validateStatusTransition(User actor, Task task, TaskStatus newStatus) {
        Set<TaskStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(task.getStatus(), EnumSet.noneOf(TaskStatus.class));
        if (!allowed.contains(newStatus)) {
            throw new IllegalStateException(
                    "Invalid transition from " + task.getStatus() + " to " + newStatus);
        }

        boolean isClient = actor.getId().equals(task.getClient().getId());
        boolean isWorker = task.getWorker() != null && actor.getId().equals(task.getWorker().getId());

        switch (newStatus) {
            case STARTED   -> { if (!isWorker) throw new AccessDeniedException("Only worker can start the task"); }
            case COMPLETED -> { if (!isClient) throw new AccessDeniedException("Only client can mark task as completed"); }
            case CANCELLED -> {
                if (!isClient && !isWorker) throw new AccessDeniedException("Only client or worker can cancel");
                task.setCancelledBy(isClient ? Task.CancelledBy.CLIENT : Task.CancelledBy.WORKER);
            }
            default -> {}
        }
    }

    private void recalculateWorkerStats(Long workerId) {
        workerProfileRepository.findByUserId(workerId).ifPresent(profile -> {
            Double avg   = reviewRepository.calculateAverageRating(workerId);
            Long   count = reviewRepository.countByRevieweeId(workerId);
            if (avg != null && count != null) {
                profile.setAverageRating(BigDecimal.valueOf(avg));
                profile.setTotalReviews(count.intValue());
                workerProfileRepository.save(profile);
            }
        });
    }

    private Task findOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));
    }

    public record TaskAcceptedEvent(Task task, User worker) {}
    public record TaskStatusChangedEvent(Task task) {}
    public record ReviewReceivedEvent(Review review, User reviewer) {}
}