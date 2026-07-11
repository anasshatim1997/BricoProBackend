package com.bricopro.matching;

import com.bricopro.messaging.websocket.WebSocketNotifier;
import com.bricopro.notification.entity.Notification;
import com.bricopro.notification.repository.NotificationRepository;
import com.bricopro.task.entity.Task;
import com.bricopro.task.repository.TaskRepository;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.WorkerAvailability;
import com.bricopro.user.entity.WorkerProfile;
import com.bricopro.user.entity.WorkerService;
import com.bricopro.user.repository.UserRepository;
import com.bricopro.user.repository.WorkerAvailabilityRepository;
import com.bricopro.user.service.WorkerSnapshotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RealTimeMatchingServiceTest {

    @Mock
    private WorkerAvailabilityRepository availabilityRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private WebSocketNotifier webSocketNotifier;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private WorkerSnapshotService workerSnapshotService;

    @InjectMocks
    private RealTimeMatchingService realTimeMatchingService;

    private Task task;
    private WorkerProfile profile;
    private WorkerAvailability availability;

    @BeforeEach
    void setUp() {
        task = Task.builder()
                .id(1L)
                .title("Fix leak")
                .address("Hay Hassani, Casablanca")
                .latitude(10.0)
                .longitude(20.0)
                .serviceType(WorkerProfile.ServiceType.PLUMBING)
                .scheduledDate(LocalDate.now().plusDays(1))
                .scheduledStart(LocalTime.of(9, 0))
                .scheduledEnd(LocalTime.of(17, 0))
                .status(Task.TaskStatus.SEARCHING)
                .build();

        User worker = User.builder().id(1L).build();
        profile = WorkerProfile.builder()
                .id(1L)
                .user(worker)
                .reliabilityScore(80)
                .services(Set.of(WorkerService.builder().serviceType(WorkerProfile.ServiceType.PLUMBING).build()))
                .build();

        availability = WorkerAvailability.builder()
                .id(1L)
                .workerProfile(profile)
                .date(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(18, 0))
                .status(WorkerAvailability.AvailabilityStatus.AVAILABLE)
                .build();
    }

    @Test
    void findMatchingWorkers_returnsList() {
        when(availabilityRepository.findAvailableWorkers(
                anyDouble(), anyDouble(), anyDouble(), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class)))
                .thenReturn(List.of(availability));

        List<Long> result = realTimeMatchingService.findMatchingWorkers(task);

        assertThat(result).containsExactly(1L);
    }

    @Test
    void findMatchingWorkers_noAvailability_returnsEmpty() {
        when(availabilityRepository.findAvailableWorkers(
                anyDouble(), anyDouble(), anyDouble(), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class)))
                .thenReturn(List.of());

        List<Long> result = realTimeMatchingService.findMatchingWorkers(task);

        assertThat(result).isEmpty();
    }

    @Test
    void findMatchingWorkers_workerHasWrongService_returnsEmpty() {
        profile.setServices(Set.of(WorkerService.builder().serviceType(WorkerProfile.ServiceType.CLEANING).build()));
        when(availabilityRepository.findAvailableWorkers(
                anyDouble(), anyDouble(), anyDouble(), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class)))
                .thenReturn(List.of(availability));

        List<Long> result = realTimeMatchingService.findMatchingWorkers(task);

        assertThat(result).isEmpty();
    }

    @Test
    void findMatchingWorkers_workerLowReliability_returnsEmpty() {
        profile.setReliabilityScore(60);
        when(availabilityRepository.findAvailableWorkers(
                anyDouble(), anyDouble(), anyDouble(), any(LocalDate.class), any(LocalTime.class), any(LocalTime.class)))
                .thenReturn(List.of(availability));

        List<Long> result = realTimeMatchingService.findMatchingWorkers(task);

        assertThat(result).isEmpty();
    }

    @Test
    void notifyWorkers_savesNotificationAndPushesForEachWorker() {
        List<Long> workerIds = List.of(1L, 2L);
        User worker1 = User.builder().id(1L).build();
        User worker2 = User.builder().id(2L).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(worker1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(worker2));

        realTimeMatchingService.notifyWorkers(workerIds, task);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(Notification::getUser)
                .containsExactly(worker1, worker2);
        assertThat(captor.getAllValues())
                .allSatisfy(n -> {
                    assertThat(n.getType()).isEqualTo(Notification.NotificationType.NEW_TASK);
                    assertThat(n.getReferenceId()).isEqualTo(1L);
                    assertThat(n.getReferenceType()).isEqualTo("TASK");
                });
        verify(webSocketNotifier, times(2)).notifyMatch(anyLong(), any(Task.class));
    }

    @Test
    void notifyWorkers_skipsMissingUsers() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        realTimeMatchingService.notifyWorkers(List.of(1L), task);

        verifyNoInteractions(notificationRepository);
        verifyNoInteractions(webSocketNotifier);
    }

    @Test
    void autoAssign_assignsFirstWorker() {
        List<Long> workerIds = List.of(1L, 2L);
        User worker = User.builder().id(1L).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(worker));
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        realTimeMatchingService.autoAssign(task, workerIds);

        assertThat(task.getWorker()).isEqualTo(worker);
        assertThat(task.getStatus()).isEqualTo(Task.TaskStatus.CONFIRMED);
        verify(userRepository).findById(1L);
        verify(workerSnapshotService).captureOnAssignment(1L, task.getId());
    }

    @Test
    void autoAssign_emptyList_doesNothing() {
        realTimeMatchingService.autoAssign(task, List.of());

        assertThat(task.getWorker()).isNull();
        assertThat(task.getStatus()).isEqualTo(Task.TaskStatus.SEARCHING);
        verifyNoInteractions(userRepository);
        verifyNoInteractions(workerSnapshotService);
    }

    @Test
    void autoAssign_workerNotFound_throwsException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> realTimeMatchingService.autoAssign(task, List.of(1L)));

        verifyNoInteractions(taskRepository);
        verifyNoInteractions(workerSnapshotService);
    }
}