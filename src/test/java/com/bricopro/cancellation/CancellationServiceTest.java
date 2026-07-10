package com.bricopro.cancellation;

import com.bricopro.notification.service.CommunicationService;
import com.bricopro.task.entity.Task;
import com.bricopro.task.entity.Task.CancelledBy;
import com.bricopro.task.entity.Task.TaskStatus;
import com.bricopro.task.repository.TaskRepository;
import com.bricopro.task.service.CancellationService;
import com.bricopro.task.service.CancellationService.CancellationResult;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.User.Role;
import com.bricopro.user.entity.User.Status;
import com.bricopro.user.entity.WorkerProfile;
import com.bricopro.user.entity.WorkerProfile.ServiceType;
import com.bricopro.user.repository.WorkerProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CancellationService")
class CancellationServiceTest {

    @Mock TaskRepository taskRepository;
    @Mock WorkerProfileRepository workerProfileRepository;
    @Mock CommunicationService communicationService;

    @InjectMocks CancellationService cancellationService;

    private User client;
    private User worker;
    private Task confirmedTask;
    private WorkerProfile workerProfile;

    @BeforeEach
    void setup() {
        client = User.builder().id(1L).firstName("Nadia").lastName("Boujemaa")
                .email("nadia@test.ma").role(Role.CLIENT).status(Status.ACTIVE).build();

        worker = User.builder().id(2L).firstName("Omar").lastName("Chraibi")
                .email("omar@test.ma").role(Role.WORKER).status(Status.ACTIVE).build();

        confirmedTask = Task.builder()
                .id(10L)
                .client(client)
                .worker(worker)
                .serviceType(ServiceType.REPAIRS)
                .title("Fix electricity")
                .description("Light switch not working")
                .address("Hay Hassani, Casablanca")
                .scheduledDate(LocalDate.now().plusDays(2))
                .scheduledStart(LocalTime.of(10, 0))
                .status(TaskStatus.CONFIRMED)
                .agreedPrice(BigDecimal.valueOf(300))
                .build();

        workerProfile = WorkerProfile.builder()
                .id(1L)
                .user(worker)
                .cancellationCount(0)
                .responseRate(BigDecimal.valueOf(95))
                .build();
    }

    // ─── CLIENT CANCELLATION ──────────────────────────────────────────────────

    @Nested
    @DisplayName("cancel() — CLIENT")
    class ClientCancellation {

        @Test
        @DisplayName("client cancelling confirmed task incurs 5% penalty")
        void clientPenalty5Percent() {
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CancellationResult result = cancellationService.cancel(confirmedTask, 1L, "Change of plans");

            assertThat(result.cancelledBy()).isEqualTo("CLIENT");
            assertThat(result.penaltyApplied()).isTrue();
            // 300 * 5% = 15.00
            assertThat(result.penaltyAmount()).isEqualByComparingTo(new BigDecimal("15.00"));
            assertThat(confirmedTask.getStatus()).isEqualTo(TaskStatus.CANCELLED);
            assertThat(confirmedTask.getCancelledBy()).isEqualTo(CancelledBy.CLIENT);
        }

        @Test
        @DisplayName("client cancelling SEARCHING task has no penalty")
        void clientNopenaltyWhenSearching() {
            confirmedTask.setStatus(TaskStatus.SEARCHING);
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CancellationResult result = cancellationService.cancel(confirmedTask, 1L, "No longer needed");

            assertThat(result.penaltyApplied()).isFalse();
            assertThat(result.penaltyAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("notifies worker when client cancels confirmed task")
        void notifiesWorker() {
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            cancellationService.cancel(confirmedTask, 1L, "Emergency");

            verify(communicationService).sendEmail(eq("omar@test.ma"), anyString(), anyString());
        }
    }

    // ─── WORKER CANCELLATION ──────────────────────────────────────────────────

    @Nested
    @DisplayName("cancel() — WORKER")
    class WorkerCancellation {

        @Test
        @DisplayName("worker cancelling confirmed task incurs 10% penalty and increments cancellation count")
        void workerPenalty10Percent() {
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(workerProfile));
            when(workerProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CancellationResult result = cancellationService.cancel(confirmedTask, 2L, "Family emergency");

            assertThat(result.cancelledBy()).isEqualTo("WORKER");
            assertThat(result.penaltyApplied()).isTrue();
            // 300 * 10% = 30.00
            assertThat(result.penaltyAmount()).isEqualByComparingTo(new BigDecimal("30.00"));
            assertThat(workerProfile.getCancellationCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("worker response rate decreases by 5 after late cancellation")
        void responseRateDecreases() {
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(workerProfile));
            when(workerProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            cancellationService.cancel(confirmedTask, 2L, "Sick");

            assertThat(workerProfile.getResponseRate().doubleValue()).isEqualTo(90.0);
        }

        @Test
        @DisplayName("worker account suspended after 3 cancellations")
        void suspendAfterThreeCancellations() {
            workerProfile.setCancellationCount(2); // already 2, this will make 3

            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(workerProfile));
            when(workerProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            cancellationService.cancel(confirmedTask, 2L, "Third cancellation");

            assertThat(worker.getStatus()).isEqualTo(Status.SUSPENDED);
            verify(communicationService).sendEmail(eq("omar@test.ma"),
                    contains("suspendu"), anyString());
        }

        @Test
        @DisplayName("notifies client when worker cancels confirmed task")
        void notifiesClient() {
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(workerProfile));
            when(workerProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            cancellationService.cancel(confirmedTask, 2L, "Unavailable");

            verify(communicationService).sendEmail(eq("nadia@test.ma"), anyString(), anyString());
        }

        @Test
        @DisplayName("response rate does not go below 0")
        void responseRateFloorAtZero() {
            workerProfile.setResponseRate(BigDecimal.valueOf(3.0));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(workerProfile));
            when(workerProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            cancellationService.cancel(confirmedTask, 2L, "Again");

            assertThat(workerProfile.getResponseRate().doubleValue()).isGreaterThanOrEqualTo(0.0);
        }
    }
}
