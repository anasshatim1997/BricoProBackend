package com.bricopro.tracking.service;

import com.bricopro.task.entity.Task.TaskStatus;
import com.bricopro.task.repository.TaskRepository;
import com.bricopro.tracking.dto.LocationUpdateRequest;
import com.bricopro.tracking.entity.WorkerLocation;
import com.bricopro.tracking.repository.WorkerLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WorkerTrackingService {

    private static final List<TaskStatus> TRACKABLE_STATUSES = List.of(TaskStatus.CONFIRMED, TaskStatus.STARTED);

    private final WorkerLocationRepository locationRepository;
    private final TaskRepository           taskRepository;
    private final SimpMessagingTemplate    messagingTemplate;

    @Transactional
    public void updateLocation(Long workerId, LocationUpdateRequest req) {
        WorkerLocation loc = locationRepository.findByWorkerId(workerId)
                .orElse(WorkerLocation.builder().workerId(workerId).build());

        loc.setLatitude(req.getLatitude());
        loc.setLongitude(req.getLongitude());
        loc.setSpeedKmh(req.getSpeedKmh());
        loc.setHeadingDegrees(req.getHeadingDegrees());
        locationRepository.save(loc);

        if (req.getClientId() != null) {
            messagingTemplate.convertAndSendToUser(
                    req.getClientId().toString(),
                    "/queue/worker-location",
                    Map.of(
                            "workerId",  workerId,
                            "latitude",  req.getLatitude(),
                            "longitude", req.getLongitude(),
                            "speed",     req.getSpeedKmh() != null ? req.getSpeedKmh() : 0,
                            "heading",   req.getHeadingDegrees() != null ? req.getHeadingDegrees() : 0,
                            "updatedAt", LocalDateTime.now().toString()
                    )
            );
        }
    }

    public Optional<WorkerLocation> getLocation(Long workerId, Long requesterId) {
        boolean isTheWorkerThemselves = workerId.equals(requesterId);
        boolean hasActiveTaskWithWorker = taskRepository
                .existsByClientIdAndWorkerIdAndStatusIn(requesterId, workerId, TRACKABLE_STATUSES);

        if (!isTheWorkerThemselves && !hasActiveTaskWithWorker) {
            throw new SecurityException("You don't have an active task with this worker");
        }

        return locationRepository.findByWorkerId(workerId);
    }
}
