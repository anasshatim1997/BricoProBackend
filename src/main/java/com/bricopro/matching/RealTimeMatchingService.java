package com.bricopro.matching;

import com.bricopro.messaging.websocket.WebSocketNotifier;
import com.bricopro.notification.entity.Notification;
import com.bricopro.notification.repository.NotificationRepository;
import com.bricopro.task.entity.Task;
import com.bricopro.task.repository.TaskRepository;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.WorkerAvailability;
import com.bricopro.user.entity.WorkerProfile;
import com.bricopro.user.entity.WorkerProfile.ServiceType;
import com.bricopro.user.repository.UserRepository;
import com.bricopro.user.repository.WorkerAvailabilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RealTimeMatchingService {

    private final WorkerAvailabilityRepository availabilityRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final WebSocketNotifier webSocketNotifier;
    private final TaskRepository taskRepository;
    private final com.bricopro.user.service.WorkerSnapshotService workerSnapshotService;

    @Transactional
    public List<Long> findMatchingWorkers(Task task) {
        double taskLat = task.getLatitude();
        double taskLng = task.getLongitude();
        double radiusKm = 20.0;
        LocalDate date = task.getScheduledDate();
        LocalTime start = task.getScheduledStart();
        LocalTime end = task.getScheduledEnd();

        List<WorkerAvailability> availabilities = availabilityRepository.findAvailableWorkers(
                taskLat, taskLng, radiusKm, date, start, end);

        return availabilities.stream()
                .filter(avail -> hasServiceType(avail.getWorkerProfile(), task.getServiceType()))
                .filter(avail -> meetsReliability(avail.getWorkerProfile()))
                .map(avail -> avail.getWorkerProfile().getUser().getId())
                .collect(Collectors.toList());
    }

    private boolean hasServiceType(WorkerProfile profile, ServiceType requiredType) {
        if (profile.getServices() == null) return false;
        return profile.getServices().stream()
                .anyMatch(service -> service.getServiceType() == requiredType);
    }

    private boolean meetsReliability(WorkerProfile profile) {
        return profile.getReliabilityScore() >= 70;
    }

    public void notifyWorkers(List<Long> workerIds, Task task) {
        for (Long workerId : workerIds) {
            userRepository.findById(workerId).ifPresent(user -> {
                notificationRepository.save(Notification.builder()
                        .user(user)
                        .type(Notification.NotificationType.NEW_TASK)
                        .title("New task available")
                        .body(task.getTitle() + " — " + task.getAddress())
                        .referenceId(task.getId())
                        .referenceType("TASK")
                        .build());
                webSocketNotifier.notifyMatch(workerId, task);
            });
        }
    }

    @Transactional
    public void autoAssign(Task task, List<Long> workerIds) {
        if (workerIds.isEmpty()) return;
        Long bestWorker = workerIds.get(0);
        User worker = userRepository.findById(bestWorker)
                .orElseThrow(() -> new RuntimeException("Worker not found"));
        task.setWorker(worker);
        task.setStatus(Task.TaskStatus.CONFIRMED);
        taskRepository.save(task);
        workerSnapshotService.captureOnAssignment(worker.getId(), task.getId());
    }
}