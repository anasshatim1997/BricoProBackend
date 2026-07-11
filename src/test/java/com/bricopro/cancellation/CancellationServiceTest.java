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
import com.bricopro.user.repository.UserRepository;
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
import java.time.LocalDateTime;
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
    @Mock UserRepository userRepository;

    @InjectMocks CancellationService cancellationService;

    private User client;
    private User worker;
    private Task task;
    private WorkerProfile workerProfile;

    @BeforeEach
    void setup() {
        client = User.builder().id(1L).firstName("Nadia").lastName("Boujemaa")
                .email("nadia@test.ma").role(Role.CLIENT).status(Status.ACTIVE)
                .cancellationCountThisMonth(0).reliabilityScore(100).build();

        worker = User.builder().id(2L).firstName("Omar").lastName("Chraibi")
                .email("omar@test.ma").role(Role.WORKER).status(Status.ACTIVE)
                .cancellationCountThisMonth(0).reliabilityScore(100).build();

        task = Task.builder()
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
                .verifiedBadge(true)
                .build();

        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private void scheduleIn(LocalDateTime target) {
        task.setScheduledDate(target.toLocalDate());
        task.setScheduledStart(target.toLocalTime());
    }

    // ─── CLIENT CANCELLATION ──────────────────────────────────────────────────

    @Nested
    @DisplayName("cancel() — CLIENT")
    class ClientCancellation {

        @Test
        @DisplayName("client cancelling more than 2h before start is free")
        void freeCancellationOverTwoHours() {
            scheduleIn(LocalDateTime.now().plusHours(5));

            CancellationResult result = cancellationService.cancel(task, 1L, "Change of plans");

            assertThat(result.cancelledBy()).isEqualTo("CLIENT");
            assertThat(result.forgiven()).isTrue();
            assertThat(result.penaltyApplied()).isFalse();
            assertThat(result.penaltyAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(task.getStatus()).isEqualTo(TaskStatus.CANCELLED);
            assertThat(task.getCancelledBy()).isEqualTo(CancelledBy.CLIENT);
            verifyNoInteractions(communicationService);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("client's first cancellation within 30min-2h window is forgiven")
        void firstLateCancellationForgiven() {
            scheduleIn(LocalDateTime.now().plusHours(1).plusMinutes(30));

            CancellationResult result = cancellationService.cancel(task, 1L, "No longer needed");

            assertThat(result.forgiven()).isTrue();
            assertThat(result.penaltyApplied()).isFalse();
            assertThat(result.penaltyAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            verifyNoInteractions(communicationService);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("client's repeat cancellation within 30min-2h window loses 5 MAD")
        void repeatLateCancellationLosesCredit() {
            client.setCancellationCountThisMonth(1);
            scheduleIn(LocalDateTime.now().plusHours(1).plusMinutes(30));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CancellationResult result = cancellationService.cancel(task, 1L, "Change of plans");

            assertThat(result.forgiven()).isFalse();
            assertThat(result.penaltyApplied()).isTrue();
            assertThat(result.penaltyAmount()).isEqualByComparingTo(new BigDecimal("5"));
            assertThat(client.getCancellationCountThisMonth()).isEqualTo(2);
            assertThat(client.getReliabilityScore()).isEqualTo(95);
            verify(communicationService).sendEmail(eq("nadia@test.ma"), anyString(), contains("5"));
        }

        @Test
        @DisplayName("client cancelling within 30min of start pays last-minute penalty capped at 20 MAD")
        void lastMinutePenaltyCapped() {
            scheduleIn(LocalDateTime.now().plusMinutes(10));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CancellationResult result = cancellationService.cancel(task, 1L, "Emergency");

            assertThat(result.forgiven()).isFalse();
            assertThat(result.penaltyApplied()).isTrue();
            // 300 * 15% = 45, capped at 20
            assertThat(result.penaltyAmount()).isEqualByComparingTo(new BigDecimal("20.00"));
            assertThat(client.getReliabilityScore()).isEqualTo(95);
            verify(communicationService).sendEmail(eq("nadia@test.ma"), anyString(), anyString());
        }

        @Test
        @DisplayName("client reliability score never goes below 0")
        void reliabilityScoreFloorAtZero() {
            client.setReliabilityScore(2);
            client.setCancellationCountThisMonth(1);
            scheduleIn(LocalDateTime.now().plusMinutes(10));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            cancellationService.cancel(task, 1L, "Emergency");

            assertThat(client.getReliabilityScore()).isEqualTo(0);
        }
    }

    // ─── WORKER CANCELLATION ──────────────────────────────────────────────────

    @Nested
    @DisplayName("cancel() — WORKER")
    class WorkerCancellation {

        @Test
        @DisplayName("worker's first cancellation this month is forgiven with no penalty or email")
        void firstCancellationForgiven() {
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(workerProfile));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CancellationResult result = cancellationService.cancel(task, 2L, "Family emergency");

            assertThat(result.cancelledBy()).isEqualTo("WORKER");
            assertThat(result.forgiven()).isTrue();
            assertThat(result.penaltyApplied()).isFalse();
            assertThat(result.reputationLoss()).isEqualTo(0);
            assertThat(worker.getCancellationCountThisMonth()).isEqualTo(1);
            assertThat(worker.getReliabilityScore()).isEqualTo(100);
            verifyNoInteractions(communicationService);
        }

        @Test
        @DisplayName("worker's second cancellation this month reduces visibility for 48h")
        void secondCancellationReducesVisibility() {
            worker.setCancellationCountThisMonth(1);
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(workerProfile));
            when(workerProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CancellationResult result = cancellationService.cancel(task, 2L, "Sick");

            assertThat(result.visibilityReduction()).isTrue();
            assertThat(result.badgeLost()).isFalse();
            assertThat(result.reputationLoss()).isEqualTo(10);
            assertThat(worker.getReliabilityScore()).isEqualTo(90);
            assertThat(worker.getCancellationCountThisMonth()).isEqualTo(2);
            assertThat(workerProfile.getVisibilityReductionUntil()).isAfter(LocalDateTime.now().plusHours(47));
            verifyNoInteractions(communicationService);
        }

        @Test
        @DisplayName("worker's fourth cancellation this month loses verified badge")
        void fourthCancellationLosesBadge() {
            worker.setCancellationCountThisMonth(3);
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(workerProfile));
            when(workerProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CancellationResult result = cancellationService.cancel(task, 2L, "Third cancellation");

            assertThat(result.badgeLost()).isTrue();
            assertThat(result.visibilityReduction()).isFalse();
            assertThat(result.reputationLoss()).isEqualTo(20);
            assertThat(worker.getReliabilityScore()).isEqualTo(80);
            assertThat(worker.getCancellationCountThisMonth()).isEqualTo(4);
            assertThat(workerProfile.isVerifiedBadge()).isFalse();
            verifyNoInteractions(communicationService);
        }

        @Test
        @DisplayName("worker reliability score never goes below 0")
        void reliabilityScoreFloorAtZero() {
            worker.setReliabilityScore(5);
            worker.setCancellationCountThisMonth(3);
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(workerProfile));
            when(workerProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            cancellationService.cancel(task, 2L, "Again");

            assertThat(worker.getReliabilityScore()).isEqualTo(0);
        }

        @Test
        @DisplayName("worker cancelling a non-confirmed task has no penalty and no profile changes")
        void noPenaltyWhenTaskNotConfirmed() {
            task.setStatus(TaskStatus.SEARCHING);

            CancellationResult result = cancellationService.cancel(task, 2L, "Not needed anymore");

            assertThat(result.cancelledBy()).isEqualTo("WORKER");
            assertThat(result.forgiven()).isFalse();
            assertThat(result.penaltyApplied()).isFalse();
            assertThat(result.reputationLoss()).isEqualTo(0);
            assertThat(worker.getCancellationCountThisMonth()).isEqualTo(0);
            assertThat(worker.getReliabilityScore()).isEqualTo(100);
            verifyNoInteractions(workerProfileRepository);
            verifyNoInteractions(communicationService);
            verify(userRepository, never()).save(any());
        }
    }
}