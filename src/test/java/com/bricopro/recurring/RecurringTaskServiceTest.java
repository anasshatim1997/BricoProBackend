package com.bricopro.recurring;

import com.bricopro.subscription.CreateRecurringRequest;
import com.bricopro.subscription.RecurringTask;
import com.bricopro.subscription.RecurringTaskRepository;
import com.bricopro.subscription.RecurringTaskService;
import com.bricopro.task.entity.Task.TaskStatus;
import com.bricopro.task.repository.TaskRepository;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.User.Role;
import com.bricopro.user.entity.User.Status;
import com.bricopro.user.entity.WorkerProfile.ServiceType;
import com.bricopro.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecurringTaskService")
class RecurringTaskServiceTest {

    @Mock RecurringTaskRepository recurringRepo;
    @Mock TaskRepository taskRepository;
    @Mock UserRepository userRepository;

    @InjectMocks RecurringTaskService recurringTaskService;

    private User clientUser;
    private User workerUser;
    private RecurringTask activeRecurring;

    @BeforeEach
    void setup() {
        clientUser = User.builder().id(1L).firstName("Sana").lastName("El Fassi")
                .email("sana@test.ma").role(Role.CLIENT).status(Status.ACTIVE).build();

        workerUser = User.builder().id(2L).firstName("Nabil").lastName("Berrada")
                .email("nabil@test.ma").role(Role.WORKER).status(Status.ACTIVE).build();

        activeRecurring = RecurringTask.builder()
                .id(10L).client(clientUser).preferredWorker(null)
                .serviceType(ServiceType.CLEANING).title("Weekly cleaning")
                .description("Full apartment clean").address("Anfa, Casablanca")
                .frequency(RecurringTask.Frequency.WEEKLY)
                .preferredTime(LocalTime.of(9, 0))
                .nextScheduledDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(6))
                .status(RecurringTask.RecurringStatus.ACTIVE)
                .build();
    }

    // ─── CREATE ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("creates recurring task with ACTIVE status")
        void createsActive() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(clientUser));
            when(recurringRepo.save(any())).thenAnswer(inv -> {
                RecurringTask rt = inv.getArgument(0);
                rt.setId(99L);
                return rt;
            });

            CreateRecurringRequest req = new CreateRecurringRequest();
            req.setServiceType(ServiceType.CLEANING);
            req.setTitle("Weekly cleaning");
            req.setDescription("Full apartment clean");
            req.setAddress("Anfa, Casablanca");
            req.setFrequency(RecurringTask.Frequency.WEEKLY);
            req.setPreferredTime(LocalTime.of(9, 0));
            req.setStartDate(LocalDate.now().plusDays(7));
            req.setEndDate(LocalDate.now().plusMonths(6));

            RecurringTask result = recurringTaskService.create(1L, req);

            assertThat(result.getId()).isEqualTo(99L);
            assertThat(result.getStatus()).isEqualTo(RecurringTask.RecurringStatus.ACTIVE);
            assertThat(result.getFrequency()).isEqualTo(RecurringTask.Frequency.WEEKLY);
        }

        @Test
        @DisplayName("throws when client not found")
        void clientNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> recurringTaskService.create(99L, new CreateRecurringRequest()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ─── PAUSE ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("pause()")
    class Pause {

        @Test
        @DisplayName("pauses an active recurring task")
        void pausesActive() {
            when(recurringRepo.findById(10L)).thenReturn(Optional.of(activeRecurring));
            when(recurringRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            RecurringTask result = recurringTaskService.pause(10L, 1L);

            assertThat(result.getStatus()).isEqualTo(RecurringTask.RecurringStatus.PAUSED);
        }

        @Test
        @DisplayName("throws SecurityException for wrong client")
        void wrongClientThrows() {
            when(recurringRepo.findById(10L)).thenReturn(Optional.of(activeRecurring));

            assertThatThrownBy(() -> recurringTaskService.pause(10L, 99L))
                    .isInstanceOf(SecurityException.class);
        }

        @Test
        @DisplayName("throws when recurring task not found")
        void notFound() {
            when(recurringRepo.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> recurringTaskService.pause(999L, 1L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ─── CANCEL ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("cancel()")
    class Cancel {

        @Test
        @DisplayName("cancels an active recurring task")
        void cancelsActive() {
            when(recurringRepo.findById(10L)).thenReturn(Optional.of(activeRecurring));
            when(recurringRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            RecurringTask result = recurringTaskService.cancel(10L, 1L);

            assertThat(result.getStatus()).isEqualTo(RecurringTask.RecurringStatus.CANCELLED);
        }

        @Test
        @DisplayName("throws SecurityException when client does not own the task")
        void wrongClientThrows() {
            when(recurringRepo.findById(10L)).thenReturn(Optional.of(activeRecurring));

            assertThatThrownBy(() -> recurringTaskService.cancel(10L, 55L))
                    .isInstanceOf(SecurityException.class);
        }
    }

    // ─── PROCESS RECURRING TASKS (SCHEDULED JOB) ──────────────────────────────

    @Nested
    @DisplayName("processRecurringTasks()")
    class ProcessRecurringTasks {

        @Test
        @DisplayName("creates a Task for each due recurring entry")
        void createsTaskForDueEntry() {
            activeRecurring.setNextScheduledDate(LocalDate.now()); // due today
            when(recurringRepo.findByStatusAndNextScheduledDateLessThanEqual(
                    RecurringTask.RecurringStatus.ACTIVE, LocalDate.now())).thenReturn(List.of(activeRecurring));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(recurringRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            recurringTaskService.processRecurringTasks();

            verify(taskRepository).save(argThat(t ->
                    t.getTitle().contains("récurrent") &&
                    t.getStatus() == TaskStatus.SEARCHING
            ));
        }

        @Test
        @DisplayName("next date advances by 1 week for WEEKLY frequency")
        void advancesNextDateWeekly() {
            LocalDate originalDate = LocalDate.now();
            activeRecurring.setNextScheduledDate(originalDate);
            activeRecurring.setFrequency(RecurringTask.Frequency.WEEKLY);

            when(recurringRepo.findByStatusAndNextScheduledDateLessThanEqual(
                    RecurringTask.RecurringStatus.ACTIVE, LocalDate.now())).thenReturn(List.of(activeRecurring));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(recurringRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            recurringTaskService.processRecurringTasks();

            assertThat(activeRecurring.getNextScheduledDate())
                    .isEqualTo(originalDate.plusWeeks(1));
        }

        @Test
        @DisplayName("next date advances by 1 month for MONTHLY frequency")
        void advancesNextDateMonthly() {
            LocalDate originalDate = LocalDate.now();
            activeRecurring.setFrequency(RecurringTask.Frequency.MONTHLY);
            activeRecurring.setNextScheduledDate(originalDate);

            when(recurringRepo.findByStatusAndNextScheduledDateLessThanEqual(
                    RecurringTask.RecurringStatus.ACTIVE, LocalDate.now())).thenReturn(List.of(activeRecurring));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(recurringRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            recurringTaskService.processRecurringTasks();

            assertThat(activeRecurring.getNextScheduledDate())
                    .isEqualTo(originalDate.plusMonths(1));
        }

        @Test
        @DisplayName("next date advances by 2 weeks for BIWEEKLY frequency")
        void advancesNextDateBiweekly() {
            LocalDate originalDate = LocalDate.now();
            activeRecurring.setFrequency(RecurringTask.Frequency.BIWEEKLY);
            activeRecurring.setNextScheduledDate(originalDate);

            when(recurringRepo.findByStatusAndNextScheduledDateLessThanEqual(
                    RecurringTask.RecurringStatus.ACTIVE, LocalDate.now())).thenReturn(List.of(activeRecurring));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(recurringRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            recurringTaskService.processRecurringTasks();

            assertThat(activeRecurring.getNextScheduledDate())
                    .isEqualTo(originalDate.plusWeeks(2));
        }

        @Test
        @DisplayName("cancels recurring task when past endDate")
        void cancelsWhenPastEndDate() {
            activeRecurring.setNextScheduledDate(LocalDate.now());
            activeRecurring.setEndDate(LocalDate.now().minusDays(1)); // end date was yesterday

            when(recurringRepo.findByStatusAndNextScheduledDateLessThanEqual(
                    RecurringTask.RecurringStatus.ACTIVE, LocalDate.now())).thenReturn(List.of(activeRecurring));
            when(recurringRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            recurringTaskService.processRecurringTasks();

            assertThat(activeRecurring.getStatus()).isEqualTo(RecurringTask.RecurringStatus.CANCELLED);
            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("assigns preferred worker when set, status CONFIRMED")
        void assignsPreferredWorker() {
            activeRecurring.setPreferredWorker(workerUser);
            activeRecurring.setNextScheduledDate(LocalDate.now());

            when(recurringRepo.findByStatusAndNextScheduledDateLessThanEqual(
                    RecurringTask.RecurringStatus.ACTIVE, LocalDate.now())).thenReturn(List.of(activeRecurring));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(recurringRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            recurringTaskService.processRecurringTasks();

            verify(taskRepository).save(argThat(t ->
                    t.getWorker() != null &&
                    t.getWorker().getId().equals(2L) &&
                    t.getStatus() == TaskStatus.CONFIRMED
            ));
        }

        @Test
        @DisplayName("no tasks created when none are due")
        void noDueTasksNothingCreated() {
            when(recurringRepo.findByStatusAndNextScheduledDateLessThanEqual(
                    RecurringTask.RecurringStatus.ACTIVE, LocalDate.now())).thenReturn(List.of());

            recurringTaskService.processRecurringTasks();

            verify(taskRepository, never()).save(any());
        }
    }

    // ─── GET FOR CLIENT ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getForClient() returns paginated recurring tasks for a client")
    void getForClient() {
        PageRequest pg = PageRequest.of(0, 10);
        when(recurringRepo.findByClientId(1L, pg))
                .thenReturn(new PageImpl<>(List.of(activeRecurring)));

        var result = recurringTaskService.getForClient(1L, pg);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Weekly cleaning");
    }
}
