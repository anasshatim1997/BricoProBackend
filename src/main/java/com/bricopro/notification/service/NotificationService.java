package com.bricopro.notification.service;

import com.bricopro.notification.entity.Notification;
import com.bricopro.task.entity.Review;
import com.bricopro.task.entity.Task;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.WorkerProfile;
import com.bricopro.user.repository.WorkerProfileRepository;
import com.bricopro.notification.repository.NotificationRepository;
import com.bricopro.notification.entity.Notification.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Notification Service", description = "Business logic for Notification Service")
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final WorkerProfileRepository workerProfileRepository;
    private final com.bricopro.user.repository.UserRepository userRepository;

    /**
     * Notify Available Workers.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public void notifyAvailableWorkers(Task task) {
        List<WorkerProfile> workers = workerProfileRepository
                .findByFilters(task.getServiceType(), null, Pageable.ofSize(100))
                .getContent();

        workers.forEach(wp -> save(
                wp.getUser(),
                Notification.NotificationType.NEW_TASK,
                "New task available",
                task.getTitle() + " — " + task.getAddress(),
                task.getId(),
                "TASK"
        ));
    }

    /**
     * Notify Task Accepted.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public void notifyTaskAccepted(Task task) {
        save(task.getClient(), Notification.NotificationType.TASK_ACCEPTED,
                "Task accepted",
                task.getWorker().getFirstName() + " accepted your request",
                task.getId(), "TASK");
    }

    /**
     * Notify Status Change.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public void notifyStatusChange(Task task) {
        NotificationType type = switch (task.getStatus()) {
            case STARTED -> NotificationType.TASK_STARTED;
            case COMPLETED -> Notification.NotificationType.TASK_COMPLETED;
            case CANCELLED -> NotificationType.TASK_CANCELLED;
            default -> NotificationType.SYSTEM;
        };
        User target = task.getStatus() == Task.TaskStatus.STARTED
                ? task.getClient()
                : (task.getWorker() != null ? task.getWorker() : task.getClient());
        save(target, type, "Task update", "Task #" + task.getId() + " is now " + task.getStatus(), task.getId(), "TASK");
    }

    /**
     * Notify Payment Received.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public void notifyPaymentReceived(User worker, Long paymentId, String amount) {
        save(worker, NotificationType.PAYMENT_RECEIVED,
                "Payment received", amount + " MAD has been transferred to your account",
                paymentId, "PAYMENT");
    }

    public void notifyCashPaymentAwaitingConfirmation(User worker, Long paymentId) {
        save(worker, NotificationType.PAYMENT_AWAITING_CONFIRMATION,
                "Confirm cash payment received",
                "The client marked this task as paid in cash. Please confirm you received the payment.",
                paymentId, "PAYMENT");
    }

    public void notifyPaymentDisputed(Long paymentId, String reason) {
        userRepository.findByRole(User.Role.ADMIN).forEach(admin ->
                save(admin, NotificationType.PAYMENT_DISPUTED,
                        "Cash payment disputed",
                        "Payment #" + paymentId + " was disputed: " + reason,
                        paymentId, "PAYMENT"));
    }

    /**
     * Notify Review Received.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public void notifyReviewReceived(Review review) {
        save(review.getReviewee(), NotificationType.REVIEW_RECEIVED,
                "New review", review.getReviewer().getFirstName() + " left you a " + review.getRating() + "★ review",
                review.getId(), "REVIEW");
    }

    /**
     * Get For User.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public Page<Notification> getForUser(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * Get Unread Count.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    /**
     * Mark All Read.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public void markAllRead(Long userId) {
        notificationRepository.markAllReadByUserId(userId);
    }

    @Transactional
    /**
     * Mark Read.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public void markRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    private void save(User user, NotificationType type, String title, String body, Long refId, String refType) {
        notificationRepository.save(Notification.builder()
                .user(user).type(type).title(title).body(body)
                .referenceId(refId).referenceType(refType)
                .build());
    }
}
