package com.bricopro.booking;

import com.bricopro.booking.entity.GroupBooking;
import com.bricopro.booking.repository.GroupBookingRepository;
import com.bricopro.booking.service.GroupBookingService;
import com.bricopro.booking.entity.GroupBookingWorker;
import com.bricopro.booking.repository.GroupBookingWorkerRepository;
import com.bricopro.task.entity.Task;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GroupBookingService")
class GroupBookingServiceTest {

    @Mock GroupBookingRepository groupBookingRepository;
    @Mock GroupBookingWorkerRepository workerRepository;
    @Mock TaskRepository taskRepository;
    @Mock UserRepository userRepository;

    @InjectMocks GroupBookingService groupBookingService;

    private User clientUser;
    private User workerUser;
    private GroupBooking openBooking;

    @BeforeEach
    void setup() {
        clientUser = User.builder().id(1L).firstName("Rim").lastName("Berrada")
                .email("rim@test.ma").role(Role.CLIENT).status(Status.ACTIVE).build();

        workerUser = User.builder().id(2L).firstName("Tarik").lastName("Alami")
                .email("tarik@test.ma").role(Role.WORKER).status(Status.ACTIVE).build();

        openBooking = GroupBooking.builder()
                .id(10L).client(clientUser).serviceType(ServiceType.MOVING)
                .title("House move").description("3-bedroom apartment")
                .address("Agdal, Rabat")
                .scheduledDate(LocalDate.now().plusDays(5))
                .scheduledStart(LocalTime.of(8, 0))
                .workersNeeded(3).workersConfirmed(0)
                .budgetPerWorker(BigDecimal.valueOf(300))
                .status(GroupBooking.GroupBookingStatus.OPEN)
                .build();
    }

    // ─── CREATE ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("creates group booking with correct fields")
        void createsBooking() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(clientUser));
            when(groupBookingRepository.save(any())).thenAnswer(inv -> {
                GroupBooking gb = inv.getArgument(0);
                gb = GroupBooking.builder().id(99L).client(clientUser)
                        .serviceType(gb.getServiceType()).title(gb.getTitle())
                        .workersNeeded(gb.getWorkersNeeded()).workersConfirmed(0)
                        .status(GroupBooking.GroupBookingStatus.OPEN).build();
                return gb;
            });

            CreateGroupBookingRequest req = new CreateGroupBookingRequest();
            req.setServiceType(ServiceType.MOVING);
            req.setTitle("House move");
            req.setDescription("Full move");
            req.setAddress("Agdal, Rabat");
            req.setScheduledDate(LocalDate.now().plusDays(5));
            req.setScheduledStart(LocalTime.of(8, 0));
            req.setWorkersNeeded(3);
            req.setBudgetPerWorker(BigDecimal.valueOf(300));

            GroupBooking result = groupBookingService.create(1L, req);

            assertThat(result.getId()).isEqualTo(99L);
            assertThat(result.getWorkersConfirmed()).isEqualTo(0);
            assertThat(result.getStatus()).isEqualTo(GroupBooking.GroupBookingStatus.OPEN);
        }

        @Test
        @DisplayName("throws when workersNeeded < 2")
        void tooFewWorkers() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(clientUser));

            CreateGroupBookingRequest req = new CreateGroupBookingRequest();
            req.setWorkersNeeded(1);

            assertThatThrownBy(() -> groupBookingService.create(1L, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("2-10");
        }

        @Test
        @DisplayName("throws when workersNeeded > 10")
        void tooManyWorkers() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(clientUser));

            CreateGroupBookingRequest req = new CreateGroupBookingRequest();
            req.setWorkersNeeded(11);

            assertThatThrownBy(() -> groupBookingService.create(1L, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("2-10");
        }
    }

    // ─── WORKER JOIN ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("workerJoin()")
    class WorkerJoin {

        @Test
        @DisplayName("worker joins OPEN booking, status updates to PARTIAL")
        void joinsOpenBooking() {
            when(groupBookingRepository.findById(10L)).thenReturn(Optional.of(openBooking));
            when(workerRepository.existsByGroupBookingIdAndWorkerId(10L, 2L)).thenReturn(false);
            when(userRepository.findById(2L)).thenReturn(Optional.of(workerUser));
            when(taskRepository.save(any())).thenAnswer(inv ->
                    com.bricopro.task.entity.Task.builder().id(55L)
                            .client(clientUser).worker(workerUser)
                            .serviceType(ServiceType.MOVING).title("House move (Groupe 1/3)")
                            .description("3-bedroom apartment").address("Agdal, Rabat")
                            .scheduledDate(LocalDate.now().plusDays(5))
                            .scheduledStart(LocalTime.of(8, 0))
                            .agreedPrice(BigDecimal.valueOf(300))
                            .status(com.bricopro.task.entity.Task.TaskStatus.CONFIRMED)
                            .build()
            );
            when(workerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(groupBookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            groupBookingService.workerJoin(2L, 10L);

            assertThat(openBooking.getWorkersConfirmed()).isEqualTo(1);
            assertThat(openBooking.getStatus()).isEqualTo(GroupBooking.GroupBookingStatus.PARTIAL);
        }

        @Test
        @DisplayName("status becomes CONFIRMED when last slot is filled")
        void lastSlotConfirms() {
            openBooking.setWorkersNeeded(2);
            openBooking.setWorkersConfirmed(1); // one already joined

            when(groupBookingRepository.findById(10L)).thenReturn(Optional.of(openBooking));
            when(workerRepository.existsByGroupBookingIdAndWorkerId(10L, 2L)).thenReturn(false);
            when(userRepository.findById(2L)).thenReturn(Optional.of(workerUser));
            when(taskRepository.save(any())).thenAnswer(inv -> {
                return com.bricopro.task.entity.Task.builder().id(56L)
                        .client(clientUser).worker(workerUser)
                        .serviceType(ServiceType.MOVING).title("House move")
                        .description("d").address("a")
                        .scheduledDate(LocalDate.now().plusDays(5))
                        .scheduledStart(LocalTime.of(8, 0))
                        .agreedPrice(BigDecimal.valueOf(300))
                        .status(com.bricopro.task.entity.Task.TaskStatus.CONFIRMED)
                        .build();
            });
            when(workerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(groupBookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            groupBookingService.workerJoin(2L, 10L);

            assertThat(openBooking.getStatus()).isEqualTo(GroupBooking.GroupBookingStatus.CONFIRMED);
        }

        @Test
        @DisplayName("throws when booking is no longer accepting workers")
        void bookingClosed() {
            openBooking.setStatus(GroupBooking.GroupBookingStatus.CONFIRMED);
            when(groupBookingRepository.findById(10L)).thenReturn(Optional.of(openBooking));

            assertThatThrownBy(() -> groupBookingService.workerJoin(2L, 10L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no longer accepting");
        }

        @Test
        @DisplayName("throws when booking is already full")
        void bookingFull() {
            openBooking.setWorkersNeeded(3);
            openBooking.setWorkersConfirmed(3);
            when(groupBookingRepository.findById(10L)).thenReturn(Optional.of(openBooking));

            assertThatThrownBy(() -> groupBookingService.workerJoin(2L, 10L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("full");
        }

        @Test
        @DisplayName("throws when worker already joined")
        void alreadyJoined() {
            when(groupBookingRepository.findById(10L)).thenReturn(Optional.of(openBooking));
            when(workerRepository.existsByGroupBookingIdAndWorkerId(10L, 2L)).thenReturn(true);

            assertThatThrownBy(() -> groupBookingService.workerJoin(2L, 10L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already joined");
        }
    }

    // ─── GET OPEN ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getOpen() returns OPEN and PARTIAL bookings combined")
    void getOpen() {
        GroupBooking partial = GroupBooking.builder().id(11L)
                .status(GroupBooking.GroupBookingStatus.PARTIAL).build();

        when(groupBookingRepository.findByStatus(GroupBooking.GroupBookingStatus.OPEN))
                .thenReturn(List.of(openBooking));
        when(groupBookingRepository.findByStatus(GroupBooking.GroupBookingStatus.PARTIAL))
                .thenReturn(List.of(partial));

        List<GroupBooking> result = groupBookingService.getOpen();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(GroupBooking::getId).contains(10L, 11L);
    }
}
