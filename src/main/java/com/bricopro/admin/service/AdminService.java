package com.bricopro.admin.service;

import com.bricopro.admin.dto.AdminDtos.*;
import com.bricopro.admin.dto.AdminDtos.ResolveDisputeRequest.DisputeResolution;
import com.bricopro.notification.service.CommunicationService;
import com.bricopro.notification.service.NotificationService;
import com.bricopro.task.dto.TaskDtos.TaskResponse;
import com.bricopro.task.entity.Task;
import com.bricopro.task.entity.Task.TaskStatus;
import com.bricopro.task.mapper.TaskMapper;
import com.bricopro.task.repository.TaskRepository;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.User.Role;
import com.bricopro.user.entity.User.Status;
import com.bricopro.user.entity.WorkerProfile;
import com.bricopro.user.entity.WorkerProfile.ServiceType;
import com.bricopro.user.repository.UserRepository;
import com.bricopro.user.repository.WorkerProfileRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Tag(name = "Admin Service", description = "Business logic for Admin Service")
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository          userRepository;
    private final WorkerProfileRepository workerProfileRepository;
    private final TaskRepository          taskRepository;
    private final CommunicationService    communicationService;
    private final NotificationService     notificationService;
    private final TaskMapper              taskMapper;
    private final com.bricopro.payment.repository.PaymentRepository paymentRepository;
    private final com.bricopro.payment.service.PaymentService       paymentService;
    private final com.bricopro.user.service.WorkerSnapshotService   workerSnapshotService;

    public Page<WorkerVerificationDto> getPendingVerifications(Pageable pageable) {
        return workerProfileRepository.findByCinVerifiedFalseAndUserStatus(Status.PENDING, pageable)
                .map(this::toVerificationDto);
    }

    @Transactional
    public ActionResponse verifyWorker(Long userId, VerifyWorkerRequest req) {
        WorkerProfile profile = workerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Worker not found"));

        profile.setCinVerified(true);
        workerProfileRepository.save(profile);

        User user = profile.getUser();
        user.setStatus(Status.ACTIVE);
        user.setVerified(true);
        userRepository.save(user);

        if (user.getEmail() != null)
            communicationService.sendWorkerVerifiedEmail(user.getEmail(), user.getFirstName());

        if (user.getPhone() != null)
            communicationService.sendWhatsApp(user.getPhone(),
                    "BricoPro: Votre compte est vérifié. Commencez à accepter des missions !");

        return new ActionResponse("Worker verified successfully", true);
    }

    @Transactional
    public ActionResponse rejectWorker(Long userId, RejectRequest req) {
        WorkerProfile profile = workerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Worker not found"));

        User user = profile.getUser();
        user.setStatus(Status.SUSPENDED);
        userRepository.save(user);

        if (user.getEmail() != null)
            communicationService.sendWorkerRejectedEmail(user.getEmail(), user.getFirstName(), req.getReason());

        return new ActionResponse("Worker rejected: " + req.getReason(), true);
    }

    @Transactional
    public ActionResponse suspendUser(Long userId, SuspendRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setStatus(Status.SUSPENDED);
        userRepository.save(user);

        if (user.getEmail() != null)
            communicationService.sendAccountSuspendedEmail(user.getEmail(), user.getFirstName(), req.getReason());

        return new ActionResponse("User suspended", true);
    }

    @Transactional
    public ActionResponse reactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setStatus(Status.ACTIVE);
        userRepository.save(user);
        return new ActionResponse("User reactivated", true);
    }

    public Page<DisputedTaskDto> getDisputedTasks(Pageable pageable) {
        List<Task> allDisputed = taskRepository.findByStatus(TaskStatus.DISPUTED);
        int start     = (int) pageable.getOffset();
        int end       = Math.min(start + pageable.getPageSize(), allDisputed.size());
        List<Task> pageSlice = start > allDisputed.size() ? List.of() : allDisputed.subList(start, end);
        return new PageImpl<>(pageSlice, pageable, allDisputed.size()).map(this::toDisputedDto);
    }

    @Transactional
    public ActionResponse resolveDispute(Long taskId, ResolveDisputeRequest req) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        if (task.getStatus() != TaskStatus.DISPUTED)
            throw new IllegalStateException("Task is not in disputed state");

        boolean favourClient = req.getResolution() == DisputeResolution.FAVOUR_CLIENT;
        task.setStatus(favourClient ? TaskStatus.CANCELLED : TaskStatus.COMPLETED);
        task.setCancellationReason("Admin resolution: " + req.getReason());
        taskRepository.save(task);

        String clientOutcome = favourClient ? "en votre faveur" : "en défaveur du client";
        String workerOutcome = favourClient ? "en défaveur du prestataire" : "en votre faveur";

        if (task.getClient().getEmail() != null)
            communicationService.sendDisputeResolvedEmail(
                    task.getClient().getEmail(), task.getClient().getFirstName(), taskId, clientOutcome);

        if (task.getWorker() != null && task.getWorker().getEmail() != null)
            communicationService.sendDisputeResolvedEmail(
                    task.getWorker().getEmail(), task.getWorker().getFirstName(), taskId, workerOutcome);

        return new ActionResponse("Dispute resolved: " + req.getResolution(), true);
    }

    @Transactional
    public ActionResponse resolvePaymentDispute(Long paymentId, ResolveDisputeRequest req) {
        com.bricopro.payment.entity.Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        if (payment.getStatus() != com.bricopro.payment.entity.Payment.PaymentStatus.DISPUTED)
            throw new IllegalStateException("Payment is not in disputed state");

        switch (req.getResolution()) {
            case FAVOUR_WORKER -> paymentService.completePayment(payment);
            case FAVOUR_CLIENT -> {
                payment.setStatus(com.bricopro.payment.entity.Payment.PaymentStatus.REFUNDED);
                paymentRepository.save(payment);
            }
            case REFUND, SPLIT -> {
                payment.setStatus(com.bricopro.payment.entity.Payment.PaymentStatus.REFUNDED);
                paymentRepository.save(payment);
            }
        }

        String clientOutcome = req.getResolution() == DisputeResolution.FAVOUR_WORKER
                ? "en défaveur du client" : "en votre faveur";
        String workerOutcome = req.getResolution() == DisputeResolution.FAVOUR_WORKER
                ? "en votre faveur" : "en défaveur du prestataire";

        if (payment.getClient().getEmail() != null)
            communicationService.sendPaymentDisputeResolvedEmail(
                    payment.getClient().getEmail(), payment.getClient().getFirstName(), paymentId, clientOutcome);

        if (payment.getWorker().getEmail() != null)
            communicationService.sendPaymentDisputeResolvedEmail(
                    payment.getWorker().getEmail(), payment.getWorker().getFirstName(), paymentId, workerOutcome);

        return new ActionResponse("Payment dispute resolved: " + req.getResolution(), true);
    }

    public Page<UserAdminDto> listUsers(String role, String status, String search, Pageable pageable) {
        Role   roleEnum   = role   != null ? Role.valueOf(role)     : null;
        Status statusEnum = status != null ? Status.valueOf(status) : null;
        return userRepository.search(roleEnum, statusEnum, search, pageable).map(this::toUserAdminDto);
    }

    @Transactional
    public ActionResponse deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setStatus(Status.DELETED);
        userRepository.save(user);
        return new ActionResponse("User soft-deleted", true);
    }

    @Transactional
    public TaskResponse assignTask(Long taskId, AssignTaskRequest req) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        if (task.getStatus() != TaskStatus.SEARCHING && task.getStatus() != TaskStatus.PENDING)
            throw new IllegalStateException(
                    "Task must be SEARCHING or PENDING to be manually assigned. Current: " + task.getStatus());

        User worker = userRepository.findById(req.getWorkerId())
                .orElseThrow(() -> new IllegalArgumentException("Worker not found"));

        task.setWorker(worker);
        task.setStatus(TaskStatus.CONFIRMED);
        if (req.getAgreedPrice() != null) task.setAgreedPrice(req.getAgreedPrice());
        if (req.getNotes()       != null) task.setCancellationReason("Admin note: " + req.getNotes());
        taskRepository.save(task);
        workerSnapshotService.captureOnAssignment(worker.getId(), task.getId());

        notificationService.notifyTaskAccepted(task);

        if (worker.getEmail() != null)
            communicationService.sendAdminTaskAssignedEmail(
                    worker.getEmail(),
                    worker.getFirstName(),
                    task.getTitle(),
                    task.getScheduledDate().toString(),
                    task.getAddress());

        return taskMapper.toResponse(task);
    }

    public Page<TaskResponse> listTasks(String status, String serviceType, Pageable pageable) {
        TaskStatus  taskStatus = status      != null ? TaskStatus.valueOf(status)       : null;
        ServiceType svcType    = serviceType != null ? ServiceType.valueOf(serviceType) : null;

        if (taskStatus == null && svcType == null)
            return taskRepository.findAll(pageable).map(taskMapper::toResponse);
        if (taskStatus != null && svcType == null)
            return taskRepository.findByStatus(taskStatus, pageable).map(taskMapper::toResponse);
        if (taskStatus == null)
            return taskRepository.findByServiceType(svcType, pageable).map(taskMapper::toResponse);

        return taskRepository.findByStatusAndServiceType(taskStatus, svcType, pageable)
                .map(taskMapper::toResponse);
    }

    private WorkerVerificationDto toVerificationDto(WorkerProfile p) {
        WorkerVerificationDto dto = new WorkerVerificationDto();
        dto.setUserId(p.getUser().getId());
        dto.setFirstName(p.getUser().getFirstName());
        dto.setLastName(p.getUser().getLastName());
        dto.setPhone(p.getUser().getPhone());
        dto.setEmail(p.getUser().getEmail());
        dto.setCinDocumentUrl(p.getCinDocumentUrl());
        dto.setRegisteredAt(p.getUser().getCreatedAt());
        return dto;
    }

    private DisputedTaskDto toDisputedDto(Task t) {
        DisputedTaskDto dto = new DisputedTaskDto();
        dto.setTaskId(t.getId());
        dto.setTitle(t.getTitle());
        dto.setClientName(t.getClient().getFirstName() + " " + t.getClient().getLastName());
        dto.setWorkerName(t.getWorker() != null
                ? t.getWorker().getFirstName() + " " + t.getWorker().getLastName() : "N/A");
        dto.setCancellationReason(t.getCancellationReason());
        dto.setCreatedAt(t.getCreatedAt());
        return dto;
    }

    private UserAdminDto toUserAdminDto(User u) {
        UserAdminDto dto = new UserAdminDto();
        dto.setId(u.getId());
        dto.setFirstName(u.getFirstName());
        dto.setLastName(u.getLastName());
        dto.setEmail(u.getEmail());
        dto.setPhone(u.getPhone());
        dto.setRole(u.getRole().name());
        dto.setStatus(u.getStatus().name());
        dto.setVerified(u.isVerified());
        dto.setCreatedAt(u.getCreatedAt());
        return dto;
    }
}