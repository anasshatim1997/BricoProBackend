package com.bricopro.task;

import com.bricopro.matching.RealTimeMatchingService;
import com.bricopro.notification.service.CommunicationService;
import com.bricopro.notification.service.NotificationService;
import com.bricopro.task.dto.TaskDtos.*;
import com.bricopro.task.entity.Review;
import com.bricopro.task.entity.Task;
import com.bricopro.task.entity.Task.CancelledBy;
import com.bricopro.task.entity.Task.TaskStatus;
import com.bricopro.task.mapper.TaskMapper;
import com.bricopro.task.repository.ReviewRepository;
import com.bricopro.task.repository.TaskRepository;
import com.bricopro.task.service.RatingSuspensionService;
import com.bricopro.task.service.TaskService;
import com.bricopro.task.service.TaskService.ReviewReceivedEvent;
import com.bricopro.task.service.TaskService.TaskAcceptedEvent;
import com.bricopro.task.service.TaskService.TaskStatusChangedEvent;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskService")
class TaskServiceTest {

    @Mock TaskRepository taskRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock UserRepository userRepository;
    @Mock WorkerProfileRepository workerProfileRepository;
    @Mock NotificationService notificationService;
    @Mock CommunicationService communicationService;
    @Mock TaskMapper mapper;
    @Mock RatingSuspensionService ratingSuspensionService;
    @Mock RealTimeMatchingService realTimeMatchingService;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks TaskService taskService;

    private User client;
    private User worker;
    private Task task;

    @BeforeEach
    void setup() {
        client = User.builder().id(1L).firstName("Khalid").lastName("Mouhib")
                .email("khalid@test.ma").role(Role.CLIENT).status(Status.ACTIVE).isVerified(true).build();

        worker = User.builder().id(2L).firstName("Rachid").lastName("Benjelloun")
                .email("rachid@test.ma").role(Role.WORKER).status(Status.ACTIVE).isVerified(true).build();

        task = Task.builder()
                .id(10L)
                .client(client)
                .serviceType(ServiceType.PLUMBING)
                .title("Fix leaking pipe")
                .description("Pipe in bathroom")
                .address("12 Rue Hassan II, Casablanca")
                .scheduledDate(LocalDate.now().plusDays(3))
                .scheduledStart(LocalTime.of(10, 0))
                .status(TaskStatus.SEARCHING)
                .budgetMin(BigDecimal.valueOf(200))
                .budgetMax(BigDecimal.valueOf(400))
                .build();
    }

    // ─── CREATE ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("creates task with SEARCHING status and notifies workers when auto-assign disabled")
        void createTask() {
            CreateTaskRequest req = new CreateTaskRequest();
            req.setServiceType(ServiceType.PLUMBING);
            req.setTitle("Fix leaking pipe");
            req.setDescription("Pipe in bathroom");
            req.setAddress("12 Rue Hassan II, Casablanca");
            req.setScheduledDate(LocalDate.now().plusDays(3));
            req.setScheduledStart(LocalTime.of(10, 0));
            req.setBudgetMin(BigDecimal.valueOf(200));
            req.setBudgetMax(BigDecimal.valueOf(400));

            when(taskRepository.save(any())).thenAnswer(inv -> {
                Task t = inv.getArgument(0);
                return Task.builder().id(99L).client(client)
                        .serviceType(t.getServiceType()).title(t.getTitle())
                        .description(t.getDescription()).address(t.getAddress())
                        .scheduledDate(t.getScheduledDate()).scheduledStart(t.getScheduledStart())
                        .autoAssignEnabled(false)
                        .status(TaskStatus.SEARCHING).build();
            });
            TaskResponse mockResponse = new TaskResponse();
            mockResponse.setId(99L);
            when(mapper.toResponse(any())).thenReturn(mockResponse);

            TaskResponse res = taskService.create(client, req);

            assertThat(res.getId()).isEqualTo(99L);
            verify(notificationService).notifyAvailableWorkers(any());
            verifyNoInteractions(realTimeMatchingService);
        }

        @Test
        @DisplayName("auto-assigns and skips worker-pool notification when auto-assign enabled")
        void createTaskAutoAssign() {
            CreateTaskRequest req = new CreateTaskRequest();
            req.setServiceType(ServiceType.PLUMBING);
            req.setTitle("Fix leaking pipe");
            req.setScheduledDate(LocalDate.now().plusDays(3));
            req.setScheduledStart(LocalTime.of(10, 0));
            req.setAutoAssignEnabled(true);

            Task savedTask = Task.builder().id(99L).client(client)
                    .serviceType(ServiceType.PLUMBING).title(req.getTitle())
                    .scheduledDate(req.getScheduledDate()).scheduledStart(req.getScheduledStart())
                    .autoAssignEnabled(true)
                    .status(TaskStatus.SEARCHING).build();

            when(taskRepository.save(any())).thenReturn(savedTask);
            when(realTimeMatchingService.findMatchingWorkers(any())).thenReturn(List.of(2L));
            TaskResponse mockResponse = new TaskResponse();
            mockResponse.setId(99L);
            when(mapper.toResponse(any())).thenReturn(mockResponse);

            TaskResponse res = taskService.create(client, req);

            assertThat(res.getId()).isEqualTo(99L);
            verify(realTimeMatchingService).findMatchingWorkers(any());
            verify(realTimeMatchingService).notifyWorkers(eq(List.of(2L)), any());
            verify(realTimeMatchingService).autoAssign(any(), eq(List.of(2L)));
            verify(notificationService, never()).notifyAvailableWorkers(any());
        }
    }

    // ─── ACCEPT TASK ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("acceptTask()")
    class AcceptTask {

        @Test
        @DisplayName("worker accepts SEARCHING task → CONFIRMED and publishes TaskAcceptedEvent")
        void acceptSearchingTask() {
            when(taskRepository.claimTask(10L, worker.getId(), TaskStatus.SEARCHING, TaskStatus.CONFIRMED))
                    .thenReturn(1);
            when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
            TaskResponse mockResponse = new TaskResponse();
            when(mapper.toResponse(any())).thenReturn(mockResponse);

            taskService.acceptTask(worker, 10L);

            ArgumentCaptor<TaskAcceptedEvent> captor = ArgumentCaptor.forClass(TaskAcceptedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().task()).isEqualTo(task);
            assertThat(captor.getValue().worker()).isEqualTo(worker);
        }

        @Test
        @DisplayName("throws IllegalStateException when task cannot be claimed (already taken or not found)")
        void claimFails() {
            when(taskRepository.claimTask(10L, worker.getId(), TaskStatus.SEARCHING, TaskStatus.CONFIRMED))
                    .thenReturn(0);

            assertThatThrownBy(() -> taskService.acceptTask(worker, 10L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no longer available");

            verifyNoInteractions(eventPublisher);
        }
    }

    // ─── UPDATE STATUS ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateStatus()")
    class UpdateStatus {

        @Test
        @DisplayName("worker can mark CONFIRMED task as STARTED")
        void workerStartsTask() {
            task.setStatus(TaskStatus.CONFIRMED);
            task.setWorker(worker);

            UpdateTaskStatusRequest req = new UpdateTaskStatusRequest();
            req.setStatus(TaskStatus.STARTED);

            when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toResponse(any())).thenReturn(new TaskResponse());

            taskService.updateStatus(worker, 10L, req);

            assertThat(task.getStatus()).isEqualTo(TaskStatus.STARTED);
            verify(eventPublisher).publishEvent(any(TaskStatusChangedEvent.class));
        }

        @Test
        @DisplayName("client can mark STARTED task as COMPLETED")
        void clientCompletesTask() {
            task.setStatus(TaskStatus.STARTED);
            task.setWorker(worker);

            UpdateTaskStatusRequest req = new UpdateTaskStatusRequest();
            req.setStatus(TaskStatus.COMPLETED);

            when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toResponse(any())).thenReturn(new TaskResponse());

            taskService.updateStatus(client, 10L, req);

            assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
            verify(eventPublisher).publishEvent(any(TaskStatusChangedEvent.class));
        }

        @Test
        @DisplayName("throws AccessDeniedException when worker tries to complete task")
        void workerCannotComplete() {
            task.setStatus(TaskStatus.STARTED);
            task.setWorker(worker);

            UpdateTaskStatusRequest req = new UpdateTaskStatusRequest();
            req.setStatus(TaskStatus.COMPLETED);

            when(taskRepository.findById(10L)).thenReturn(Optional.of(task));

            assertThatThrownBy(() -> taskService.updateStatus(worker, 10L, req))
                    .isInstanceOf(AccessDeniedException.class);
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("throws AccessDeniedException when client tries to start task")
        void clientCannotStart() {
            task.setStatus(TaskStatus.CONFIRMED);
            task.setWorker(worker);

            UpdateTaskStatusRequest req = new UpdateTaskStatusRequest();
            req.setStatus(TaskStatus.STARTED);

            when(taskRepository.findById(10L)).thenReturn(Optional.of(task));

            assertThatThrownBy(() -> taskService.updateStatus(client, 10L, req))
                    .isInstanceOf(AccessDeniedException.class);
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("client can cancel task and sets CancelledBy.CLIENT")
        void clientCancelsTask() {
            task.setStatus(TaskStatus.CONFIRMED);
            task.setWorker(worker);

            UpdateTaskStatusRequest req = new UpdateTaskStatusRequest();
            req.setStatus(TaskStatus.CANCELLED);
            req.setCancellationReason("Change of plans");

            when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toResponse(any())).thenReturn(new TaskResponse());

            taskService.updateStatus(client, 10L, req);

            assertThat(task.getCancelledBy()).isEqualTo(CancelledBy.CLIENT);
            verify(eventPublisher).publishEvent(any(TaskStatusChangedEvent.class));
        }

        @Test
        @DisplayName("throws IllegalStateException for invalid transition")
        void invalidTransition() {
            task.setStatus(TaskStatus.COMPLETED);
            task.setWorker(worker);

            UpdateTaskStatusRequest req = new UpdateTaskStatusRequest();
            req.setStatus(TaskStatus.STARTED);

            when(taskRepository.findById(10L)).thenReturn(Optional.of(task));

            assertThatThrownBy(() -> taskService.updateStatus(worker, 10L, req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Invalid transition");
            verifyNoInteractions(eventPublisher);
        }
    }

    // ─── SUBMIT REVIEW ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("submitReview()")
    class SubmitReview {

        @Test
        @DisplayName("client submits review for completed task and publishes ReviewReceivedEvent")
        void clientReviewsWorker() {
            task.setStatus(TaskStatus.COMPLETED);
            task.setWorker(worker);

            CreateReviewRequest req = new CreateReviewRequest();
            req.setRating(5);
            req.setComment("Excellent service!");

            Review savedReview = Review.builder()
                    .id(1L).task(task).reviewer(client).reviewee(worker)
                    .rating(5).comment("Excellent service!").build();

            when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
            when(reviewRepository.findByTaskIdAndReviewerId(10L, 1L)).thenReturn(Optional.empty());
            when(reviewRepository.save(any())).thenReturn(savedReview);
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(
                    WorkerProfile.builder().id(1L).user(worker).totalReviews(5)
                            .averageRating(BigDecimal.valueOf(4.2)).build()));
            when(workerProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(reviewRepository.calculateAverageRating(2L)).thenReturn(4.5);
            when(reviewRepository.countByRevieweeId(2L)).thenReturn(6L);
            ReviewResponse mockResponse = new ReviewResponse();
            mockResponse.setRating(5);
            when(mapper.toReviewResponse(any())).thenReturn(mockResponse);

            ReviewResponse res = taskService.submitReview(client, 10L, req);

            assertThat(res.getRating()).isEqualTo(5);
            verify(ratingSuspensionService).evaluateWorkerRating(2L);

            ArgumentCaptor<ReviewReceivedEvent> captor = ArgumentCaptor.forClass(ReviewReceivedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().review()).isEqualTo(savedReview);
            assertThat(captor.getValue().reviewer()).isEqualTo(client);
        }

        @Test
        @DisplayName("throws when task not completed")
        void notCompletedTask() {
            when(taskRepository.findById(10L)).thenReturn(Optional.of(task));

            assertThatThrownBy(() -> taskService.submitReview(client, 10L, new CreateReviewRequest()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("completed");
        }

        @Test
        @DisplayName("throws when user already reviewed this task")
        void alreadyReviewed() {
            task.setStatus(TaskStatus.COMPLETED);
            task.setWorker(worker);

            when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
            when(reviewRepository.findByTaskIdAndReviewerId(10L, 1L))
                    .thenReturn(Optional.of(new Review()));

            assertThatThrownBy(() -> taskService.submitReview(client, 10L, new CreateReviewRequest()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Already reviewed");
        }

        @Test
        @DisplayName("throws when client reviews task with no assigned worker")
        void noAssignedWorker() {
            task.setStatus(TaskStatus.COMPLETED);
            task.setWorker(null);

            when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
            when(reviewRepository.findByTaskIdAndReviewerId(10L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.submitReview(client, 10L, new CreateReviewRequest()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no assigned worker");
        }
    }

    // ─── GET TASKS ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getClientTasks() / getWorkerTasks()")
    class GetTasks {

        @Test
        @DisplayName("returns paginated client tasks filtered by status")
        void clientTasksWithStatus() {
            PageRequest pg = PageRequest.of(0, 10);
            Page<Task> page = new PageImpl<>(List.of(task));

            when(taskRepository.findByClientIdAndStatus(1L, TaskStatus.SEARCHING, pg))
                    .thenReturn(page);
            when(mapper.toResponse(any())).thenReturn(new TaskResponse());

            Page<TaskResponse> res = taskService.getClientTasks(1L, TaskStatus.SEARCHING, pg);
            assertThat(res.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("returns all client tasks when no status filter")
        void clientTasksNoFilter() {
            PageRequest pg = PageRequest.of(0, 10);
            when(taskRepository.findByClientId(1L, pg)).thenReturn(new PageImpl<>(List.of(task)));
            when(mapper.toResponse(any())).thenReturn(new TaskResponse());

            Page<TaskResponse> res = taskService.getClientTasks(1L, null, pg);
            assertThat(res.getTotalElements()).isEqualTo(1);
        }
    }

    // ─── EVENT LISTENERS (fired AFTER_COMMIT in prod, tested directly here) ───

    @Nested
    @DisplayName("event listeners")
    class EventListeners {

        @Test
        @DisplayName("onTaskAccepted notifies worker acceptance and emails client")
        void onTaskAccepted() {
            task.setWorker(worker);

            taskService.onTaskAccepted(new TaskAcceptedEvent(task, worker));

            verify(notificationService).notifyTaskAccepted(task);
            verify(communicationService).sendTaskConfirmationEmail(
                    eq("khalid@test.ma"), eq("Khalid"), anyString(), eq("Rachid Benjelloun"));
        }

        @Test
        @DisplayName("onStatusChanged sends completion email when task is COMPLETED")
        void onStatusChangedCompleted() {
            task.setStatus(TaskStatus.COMPLETED);
            task.setWorker(worker);

            taskService.onStatusChanged(new TaskStatusChangedEvent(task));

            verify(notificationService).notifyStatusChange(task);
            verify(communicationService).sendTaskCompletedEmail(
                    eq("khalid@test.ma"), eq("Khalid"), eq("Fix leaking pipe"));
        }

        @Test
        @DisplayName("onStatusChanged emails both parties when task is CANCELLED")
        void onStatusChangedCancelled() {
            task.setStatus(TaskStatus.CANCELLED);
            task.setWorker(worker);
            task.setCancelledBy(CancelledBy.CLIENT);

            taskService.onStatusChanged(new TaskStatusChangedEvent(task));

            verify(notificationService).notifyStatusChange(task);
            verify(communicationService).sendTaskCancelledEmail(
                    eq("khalid@test.ma"), eq("Khalid"), eq("Fix leaking pipe"), eq("client"));
            verify(communicationService).sendTaskCancelledEmail(
                    eq("rachid@test.ma"), eq("Rachid"), eq("Fix leaking pipe"), eq("client"));
        }

        @Test
        @DisplayName("onReviewReceived notifies reviewee and sends email")
        void onReviewReceived() {
            Review review = Review.builder()
                    .id(1L).task(task).reviewer(client).reviewee(worker)
                    .rating(5).comment("Great!").build();

            taskService.onReviewReceived(new ReviewReceivedEvent(review, client));

            verify(notificationService).notifyReviewReceived(review);
            verify(communicationService).sendReviewReceivedEmail(
                    eq("rachid@test.ma"), eq("Rachid"), eq(5), eq("Khalid Mouhib"));
        }
    }
}