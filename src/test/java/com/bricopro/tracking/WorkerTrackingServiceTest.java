package com.bricopro.tracking;

import com.bricopro.task.entity.Task.TaskStatus;
import com.bricopro.task.repository.TaskRepository;
import com.bricopro.tracking.dto.LocationUpdateRequest;
import com.bricopro.tracking.entity.WorkerLocation;
import com.bricopro.tracking.repository.WorkerLocationRepository;
import com.bricopro.tracking.service.WorkerTrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkerTrackingService")
class WorkerTrackingServiceTest {

    @Mock WorkerLocationRepository locationRepository;
    @Mock TaskRepository taskRepository;
    @Mock SimpMessagingTemplate messagingTemplate;

    @InjectMocks WorkerTrackingService trackingService;

    @Nested
    @DisplayName("updateLocation()")
    class UpdateLocation {

        @Test
        @DisplayName("creates a new location row when none exists yet")
        void createsNewLocation() {
            when(locationRepository.findByWorkerId(2L)).thenReturn(Optional.empty());
            when(locationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            LocationUpdateRequest req = new LocationUpdateRequest();
            req.setLatitude(33.5731);
            req.setLongitude(-7.5898);

            trackingService.updateLocation(2L, req);

            verify(locationRepository).save(argThat(loc ->
                    loc.getWorkerId().equals(2L) && loc.getLatitude() == 33.5731));
        }

        @Test
        @DisplayName("broadcasts to the client via WebSocket when a clientId is provided")
        void broadcastsToClient() {
            when(locationRepository.findByWorkerId(2L)).thenReturn(Optional.empty());
            when(locationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            LocationUpdateRequest req = new LocationUpdateRequest();
            req.setLatitude(33.5731);
            req.setLongitude(-7.5898);
            req.setClientId(5L);

            trackingService.updateLocation(2L, req);

            verify(messagingTemplate).convertAndSendToUser(eq("5"), eq("/queue/worker-location"), any());
        }

        @Test
        @DisplayName("does not broadcast when no clientId is provided")
        void noBroadcastWithoutClientId() {
            when(locationRepository.findByWorkerId(2L)).thenReturn(Optional.empty());
            when(locationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            LocationUpdateRequest req = new LocationUpdateRequest();
            req.setLatitude(33.5731);
            req.setLongitude(-7.5898);

            trackingService.updateLocation(2L, req);

            verifyNoInteractions(messagingTemplate);
        }
    }

    @Nested
    @DisplayName("getLocation() — access control")
    class GetLocationAccessControl {

        @Test
        @DisplayName("REGRESSION: allows the worker to see their own location")
        void allowsWorkerToSeeOwnLocation() {
            WorkerLocation loc = WorkerLocation.builder().workerId(2L).latitude(33.5).longitude(-7.5).build();
            when(taskRepository.existsByClientIdAndWorkerIdAndStatusIn(eq(2L), eq(2L), any())).thenReturn(false);
            when(locationRepository.findByWorkerId(2L)).thenReturn(Optional.of(loc));

            Optional<WorkerLocation> result = trackingService.getLocation(2L, 2L);

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("REGRESSION: allows a client with an active (CONFIRMED/STARTED) task with the worker")
        void allowsClientWithActiveTask() {
            WorkerLocation loc = WorkerLocation.builder().workerId(2L).latitude(33.5).longitude(-7.5).build();
            when(taskRepository.existsByClientIdAndWorkerIdAndStatusIn(
                    eq(9L), eq(2L), eq(List.of(TaskStatus.CONFIRMED, TaskStatus.STARTED))))
                    .thenReturn(true);
            when(locationRepository.findByWorkerId(2L)).thenReturn(Optional.of(loc));

            Optional<WorkerLocation> result = trackingService.getLocation(2L, 9L);

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("REGRESSION: rejects a random user with no relationship to the worker")
        void rejectsUnrelatedUser() {
            when(taskRepository.existsByClientIdAndWorkerIdAndStatusIn(eq(99L), eq(2L), any()))
                    .thenReturn(false);

            assertThatThrownBy(() -> trackingService.getLocation(2L, 99L))
                    .isInstanceOf(SecurityException.class);

            verify(locationRepository, never()).findByWorkerId(any());
        }

        @Test
        @DisplayName("returns empty when authorized but no location has been reported yet")
        void authorizedButNoLocationYet() {
            when(taskRepository.existsByClientIdAndWorkerIdAndStatusIn(eq(9L), eq(2L), any())).thenReturn(true);
            when(locationRepository.findByWorkerId(2L)).thenReturn(Optional.empty());

            Optional<WorkerLocation> result = trackingService.getLocation(2L, 9L);

            assertThat(result).isEmpty();
        }
    }
}
