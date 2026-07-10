package com.bricopro.booking.service;

import com.bricopro.booking.dto.CreateGroupBookingRequest;
import com.bricopro.booking.entity.GroupBooking;
import com.bricopro.booking.entity.GroupBookingWorker;
import com.bricopro.booking.repository.GroupBookingRepository;
import com.bricopro.booking.repository.GroupBookingWorkerRepository;
import com.bricopro.task.entity.Task;
import com.bricopro.task.entity.Task.TaskStatus;
import com.bricopro.task.repository.TaskRepository;
import com.bricopro.user.entity.User;
import com.bricopro.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupBookingService {

    private final GroupBookingRepository       groupBookingRepository;
    private final GroupBookingWorkerRepository workerRepository;
    private final TaskRepository               taskRepository;
    private final UserRepository               userRepository;

    @Transactional
    public GroupBooking create(Long clientId, CreateGroupBookingRequest req) {
        User client = userRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (req.getWorkersNeeded() < 2 || req.getWorkersNeeded() > 10)
            throw new IllegalArgumentException("Group booking requires 2-10 workers");

        GroupBooking gb = GroupBooking.builder()
                .client(client)
                .serviceType(req.getServiceType())
                .title(req.getTitle())
                .description(req.getDescription())
                .address(req.getAddress())
                .scheduledDate(req.getScheduledDate())
                .scheduledStart(req.getScheduledStart())
                .workersNeeded(req.getWorkersNeeded())
                .workersConfirmed(0)
                .budgetPerWorker(req.getBudgetPerWorker())
                .build();

        return groupBookingRepository.save(gb);
    }

    @Transactional
    public GroupBookingWorker workerJoin(Long workerId, Long groupBookingId) {
        GroupBooking gb = groupBookingRepository.findById(groupBookingId)
                .orElseThrow(() -> new IllegalArgumentException("Group booking not found"));

        if (gb.getStatus() != GroupBooking.GroupBookingStatus.OPEN
                && gb.getStatus() != GroupBooking.GroupBookingStatus.PARTIAL)
            throw new IllegalStateException("Group booking is no longer accepting workers");

        if (gb.getWorkersConfirmed() >= gb.getWorkersNeeded())
            throw new IllegalStateException("Group booking is already full");

        if (workerRepository.existsByGroupBookingIdAndWorkerId(groupBookingId, workerId))
            throw new IllegalStateException("You already joined this group booking");

        User worker = userRepository.findById(workerId)
                .orElseThrow(() -> new IllegalArgumentException("Worker not found"));

        Task task = Task.builder()
                .client(gb.getClient())
                .worker(worker)
                .serviceType(gb.getServiceType())
                .title(gb.getTitle() + " (Groupe " + (gb.getWorkersConfirmed() + 1) + "/" + gb.getWorkersNeeded() + ")")
                .description(gb.getDescription())
                .address(gb.getAddress())
                .scheduledDate(gb.getScheduledDate())
                .scheduledStart(gb.getScheduledStart())
                .agreedPrice(gb.getBudgetPerWorker())
                .status(TaskStatus.CONFIRMED)
                .build();
        task = taskRepository.save(task);

        GroupBookingWorker gbw = GroupBookingWorker.builder()
                .groupBooking(gb)
                .worker(worker)
                .taskId(task.getId())
                .build();
        workerRepository.save(gbw);

        gb.setWorkersConfirmed(gb.getWorkersConfirmed() + 1);
        gb.setStatus(gb.getWorkersConfirmed() >= gb.getWorkersNeeded()
                ? GroupBooking.GroupBookingStatus.CONFIRMED
                : GroupBooking.GroupBookingStatus.PARTIAL);
        groupBookingRepository.save(gb);

        return gbw;
    }

    public List<GroupBooking> getOpen() {
        List<GroupBooking> result = new ArrayList<>();
        result.addAll(groupBookingRepository.findByStatus(GroupBooking.GroupBookingStatus.OPEN));
        result.addAll(groupBookingRepository.findByStatus(GroupBooking.GroupBookingStatus.PARTIAL));
        return result;
    }

    public List<GroupBooking> getClientBookings(Long clientId) {
        return groupBookingRepository.findByClientId(clientId);
    }
}
