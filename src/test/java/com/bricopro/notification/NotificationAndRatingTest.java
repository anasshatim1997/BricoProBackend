package com.bricopro.notification;

import com.bricopro.notification.entity.Notification;
import com.bricopro.notification.entity.Notification.NotificationType;
import com.bricopro.notification.repository.NotificationRepository;
import com.bricopro.notification.service.CommunicationService;
import com.bricopro.notification.service.NotificationService;
import com.bricopro.task.entity.Review;
import com.bricopro.task.entity.Task;
import com.bricopro.task.entity.Task.TaskStatus;
import com.bricopro.task.repository.ReviewRepository;
import com.bricopro.task.service.RatingSuspensionService;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.User.Role;
import com.bricopro.user.entity.User.Status;
import com.bricopro.user.entity.WorkerProfile;
import com.bricopro.user.entity.WorkerProfile.ServiceType;
import com.bricopro.user.repository.UserRepository;
import com.bricopro.user.repository.WorkerProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Notification & Rating Suspension")
class NotificationAndRatingTest {

    // ─── NOTIFICATION SERVICE ─────────────────────────────────────────────────

    @Nested
    @DisplayName("NotificationService")
    class NotificationServiceTests {

        @Mock NotificationRepository notificationRepository;
        @Mock WorkerProfileRepository workerProfileRepository;

        @InjectMocks NotificationService notificationService;

        private User client;
        private User worker;
        private Task task;

        @BeforeEach
        void setup() {
            client = User.builder().id(1L).firstName("Hind").lastName("Chraibi")
                    .email("hind@test.ma").role(Role.CLIENT).status(Status.ACTIVE).build();

            worker = User.builder().id(2L).firstName("Anas").lastName("Mansouri")
                    .email("anas@test.ma").role(Role.WORKER).status(Status.ACTIVE).build();

            task = Task.builder()
                    .id(10L).client(client).worker(worker)
                    .serviceType(ServiceType.CLEANING)
                    .title("Home cleaning").description("d").address("a")
                    .scheduledDate(LocalDate.now()).scheduledStart(LocalTime.of(9, 0))
                    .status(TaskStatus.CONFIRMED).build();
        }

        @Test
        @DisplayName("notifyTaskAccepted saves TASK_ACCEPTED notification for client")
        void notifyTaskAccepted() {
            when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            notificationService.notifyTaskAccepted(task);

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());
            assertThat(captor.getValue().getType()).isEqualTo(NotificationType.TASK_ACCEPTED);
            assertThat(captor.getValue().getUser().getId()).isEqualTo(1L); // client
        }

        @Test
        @DisplayName("notifyStatusChange saves TASK_STARTED for started task → client is target")
        void notifyStarted() {
            task.setStatus(TaskStatus.STARTED);
            when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            notificationService.notifyStatusChange(task);

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());
            assertThat(captor.getValue().getType()).isEqualTo(NotificationType.TASK_STARTED);
            assertThat(captor.getValue().getUser().getId()).isEqualTo(1L); // client for STARTED
        }

        @Test
        @DisplayName("notifyStatusChange saves TASK_COMPLETED → targets worker")
        void notifyCompleted() {
            task.setStatus(TaskStatus.COMPLETED);
            when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            notificationService.notifyStatusChange(task);

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());
            assertThat(captor.getValue().getType()).isEqualTo(NotificationType.TASK_COMPLETED);
            assertThat(captor.getValue().getUser().getId()).isEqualTo(2L); // worker
        }

        @Test
        @DisplayName("notifyPaymentReceived saves PAYMENT_RECEIVED for worker")
        void notifyPaymentReceived() {
            when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            notificationService.notifyPaymentReceived(worker, 55L, "432.50");

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());
            assertThat(captor.getValue().getType()).isEqualTo(NotificationType.PAYMENT_RECEIVED);
            assertThat(captor.getValue().getUser().getId()).isEqualTo(2L);
            assertThat(captor.getValue().getBody()).contains("432.50");
        }

        @Test
        @DisplayName("notifyReviewReceived saves REVIEW_RECEIVED for reviewee")
        void notifyReviewReceived() {
            Review review = Review.builder()
                    .id(1L).task(task).reviewer(client).reviewee(worker)
                    .rating(5).comment("Great!").build();

            when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            notificationService.notifyReviewReceived(review);

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());
            assertThat(captor.getValue().getType()).isEqualTo(NotificationType.REVIEW_RECEIVED);
            assertThat(captor.getValue().getUser().getId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("getUnreadCount delegates to repository")
        void getUnreadCount() {
            when(notificationRepository.countByUserIdAndIsReadFalse(1L)).thenReturn(7L);
            assertThat(notificationService.getUnreadCount(1L)).isEqualTo(7L);
        }

        @Test
        @DisplayName("markAllRead calls repository bulk update")
        void markAllRead() {
            notificationService.markAllRead(1L);
            verify(notificationRepository).markAllReadByUserId(1L);
        }

        @Test
        @DisplayName("markRead marks single notification as read")
        void markSingleRead() {
            Notification notif = Notification.builder()
                    .id(1L).user(client).type(NotificationType.NEW_TASK)
                    .title("New task").body("body").isRead(false).build();

            when(notificationRepository.findById(1L)).thenReturn(Optional.of(notif));
            when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            notificationService.markRead(1L);

            assertThat(notif.isRead()).isTrue();
        }

        @Test
        @DisplayName("notifyAvailableWorkers sends notifications to nearby workers")
        void notifyAvailableWorkers() {
            WorkerProfile wp = WorkerProfile.builder().id(1L).user(worker).build();
            when(workerProfileRepository.findByFilters(eq(ServiceType.CLEANING), isNull(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(wp)));
            when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            notificationService.notifyAvailableWorkers(task);

            verify(notificationRepository).save(argThat(n ->
                    n.getType() == NotificationType.NEW_TASK &&
                    n.getUser().getId().equals(2L)));
        }

        @Test
        @DisplayName("getForUser returns paginated notifications")
        void getForUser() {
            PageRequest pg = PageRequest.of(0, 10);
            Notification notif = Notification.builder().id(1L).user(client)
                    .type(NotificationType.SYSTEM).title("t").body("b").build();
            when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L, pg))
                    .thenReturn(new PageImpl<>(List.of(notif)));

            Page<Notification> page = notificationService.getForUser(1L, pg);
            assertThat(page.getTotalElements()).isEqualTo(1);
        }
    }

    // ─── RATING SUSPENSION SERVICE ────────────────────────────────────────────

    @Nested
    @DisplayName("RatingSuspensionService")
    class RatingSuspensionServiceTests {

        @Mock WorkerProfileRepository workerProfileRepository;
        @Mock UserRepository userRepository;
        @Mock ReviewRepository reviewRepository;
        @Mock CommunicationService communicationService;

        @InjectMocks RatingSuspensionService ratingSuspensionService;

        private User workerUser;
        private WorkerProfile profile;

        @BeforeEach
        void setup() {
            workerUser = User.builder().id(2L).firstName("Jamal").lastName("Skalli")
                    .email("jamal@test.ma").role(Role.WORKER).status(Status.ACTIVE).build();

            profile = WorkerProfile.builder()
                    .id(1L).user(workerUser)
                    .totalReviews(15) // >= 10 required
                    .averageRating(BigDecimal.valueOf(4.5))
                    .build();
        }

        @Test
        @DisplayName("suspends worker when average rating < 3.0 and >= 10 reviews")
        void suspendsLowRatedWorker() {
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(profile));
            when(reviewRepository.calculateAverageRating(2L)).thenReturn(2.5);
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ratingSuspensionService.evaluateWorkerRating(2L);

            assertThat(workerUser.getStatus()).isEqualTo(Status.SUSPENDED);
            verify(communicationService).sendEmail(eq("jamal@test.ma"), anyString(), anyString());
        }

        @Test
        @DisplayName("does NOT suspend worker with rating >= 3.0")
        void doesNotSuspendGoodRating() {
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(profile));
            when(reviewRepository.calculateAverageRating(2L)).thenReturn(3.5);

            ratingSuspensionService.evaluateWorkerRating(2L);

            assertThat(workerUser.getStatus()).isEqualTo(Status.ACTIVE);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("does NOT evaluate when fewer than 10 reviews")
        void skipsWhenTooFewReviews() {
            profile.setTotalReviews(5); // below threshold
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(profile));

            ratingSuspensionService.evaluateWorkerRating(2L);

            verify(reviewRepository, never()).calculateAverageRating(any());
            assertThat(workerUser.getStatus()).isEqualTo(Status.ACTIVE);
        }

        @Test
        @DisplayName("does NOT re-suspend an already suspended worker")
        void doesNotReSuspend() {
            workerUser.setStatus(Status.SUSPENDED);
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(profile));
            when(reviewRepository.calculateAverageRating(2L)).thenReturn(2.0);

            ratingSuspensionService.evaluateWorkerRating(2L);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("sends WhatsApp notification when phone is available")
        void sendsWhatsAppIfPhone() {
            workerUser.setPhone("+212600000002");
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(profile));
            when(reviewRepository.calculateAverageRating(2L)).thenReturn(2.1);
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ratingSuspensionService.evaluateWorkerRating(2L);

            verify(communicationService).sendWhatsApp(eq("+212600000002"), anyString());
        }

        @Test
        @DisplayName("throws when worker profile not found")
        void workerNotFound() {
            when(workerProfileRepository.findByUserId(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> ratingSuspensionService.evaluateWorkerRating(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not found");
        }

        @Test
        @DisplayName("handles null average rating gracefully")
        void nullAverageRating() {
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(profile));
            when(reviewRepository.calculateAverageRating(2L)).thenReturn(null);

            // should not throw
            assertThatNoException().isThrownBy(() -> ratingSuspensionService.evaluateWorkerRating(2L));
            assertThat(workerUser.getStatus()).isEqualTo(Status.ACTIVE);
        }
    }
}
