package com.bricopro.admin;

import com.bricopro.admin.dto.AdminDtos.*;
import com.bricopro.admin.dto.AdminDtos.ResolveDisputeRequest.DisputeResolution;
import com.bricopro.admin.service.AdminService;
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
import com.bricopro.user.service.WorkerSnapshotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService")
class AdminServiceTest {

    @Mock UserRepository userRepository;
    @Mock WorkerProfileRepository workerProfileRepository;
    @Mock TaskRepository taskRepository;
    @Mock CommunicationService communicationService;
    @Mock NotificationService notificationService;
    @Mock TaskMapper taskMapper;
    @Mock WorkerSnapshotService workerSnapshotService;

    @InjectMocks AdminService adminService;

    private User workerUser;
    private User clientUser;
    private WorkerProfile workerProfile;
    private Task disputedTask;

    @BeforeEach
    void setup() {
        workerUser = User.builder().id(2L).firstName("Yassine").lastName("Kettani")
                .email("yassine@test.ma").phone("+212612345678")
                .role(Role.WORKER).status(Status.PENDING).isVerified(false).build();

        clientUser = User.builder().id(1L).firstName("Loubna").lastName("Alaoui")
                .email("loubna@test.ma").role(Role.CLIENT).status(Status.ACTIVE).build();

        workerProfile = WorkerProfile.builder()
                .id(1L).user(workerUser).cinVerified(false).build();

        disputedTask = Task.builder()
                .id(5L).client(clientUser).worker(workerUser)
                .serviceType(ServiceType.REPAIRS).title("Disputed repair")
                .description("desc").address("addr")
                .scheduledDate(LocalDate.now()).scheduledStart(LocalTime.of(9, 0))
                .status(TaskStatus.DISPUTED)
                .cancellationReason("Client says work not done")
                .build();
    }

    @Nested
    @DisplayName("verifyWorker()")
    class VerifyWorker {

        @Test
        @DisplayName("verifies worker, sets ACTIVE, sends email and WhatsApp")
        void verifiesWorker() {
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(workerProfile));
            when(workerProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            VerifyWorkerRequest req = new VerifyWorkerRequest();
            ActionResponse res = adminService.verifyWorker(2L, req);

            assertThat(res.isSuccess()).isTrue();
            assertThat(workerProfile.isCinVerified()).isTrue();
            assertThat(workerUser.getStatus()).isEqualTo(Status.ACTIVE);
            assertThat(workerUser.isVerified()).isTrue();
            verify(communicationService).sendWorkerVerifiedEmail(eq("yassine@test.ma"), eq("Yassine"));
            verify(communicationService).sendWhatsApp(eq("+212612345678"), anyString());
        }

        @Test
        @DisplayName("throws when worker profile not found")
        void workerNotFound() {
            when(workerProfileRepository.findByUserId(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.verifyWorker(99L, new VerifyWorkerRequest()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not found");
        }
    }

    @Nested
    @DisplayName("rejectWorker()")
    class RejectWorker {

        @Test
        @DisplayName("rejects worker, sets SUSPENDED, sends rejection email")
        void rejectsWorker() {
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(workerProfile));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            RejectRequest req = new RejectRequest();
            req.setReason("CIN document blurry");

            ActionResponse res = adminService.rejectWorker(2L, req);

            assertThat(res.isSuccess()).isTrue();
            assertThat(workerUser.getStatus()).isEqualTo(Status.SUSPENDED);
            verify(communicationService).sendWorkerRejectedEmail(
                    eq("yassine@test.ma"), eq("Yassine"), eq("CIN document blurry"));
        }
    }

    @Nested
    @DisplayName("suspendUser()")
    class SuspendUser {

        @Test
        @DisplayName("suspends user and sends suspension email")
        void suspendsUser() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(clientUser));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SuspendRequest req = new SuspendRequest();
            req.setReason("Fraudulent activity");

            ActionResponse res = adminService.suspendUser(1L, req);

            assertThat(res.isSuccess()).isTrue();
            assertThat(clientUser.getStatus()).isEqualTo(Status.SUSPENDED);
            verify(communicationService).sendAccountSuspendedEmail(
                    eq("loubna@test.ma"), eq("Loubna"), eq("Fraudulent activity"));
        }

        @Test
        @DisplayName("throws when user not found")
        void userNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> adminService.suspendUser(999L, new SuspendRequest()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("reactivateUser()")
    class ReactivateUser {

        @Test
        @DisplayName("reactivates suspended user to ACTIVE")
        void reactivates() {
            clientUser.setStatus(Status.SUSPENDED);
            when(userRepository.findById(1L)).thenReturn(Optional.of(clientUser));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ActionResponse res = adminService.reactivateUser(1L);

            assertThat(res.isSuccess()).isTrue();
            assertThat(clientUser.getStatus()).isEqualTo(Status.ACTIVE);
        }
    }

    @Nested
    @DisplayName("resolveDispute()")
    class ResolveDispute {

        @Test
        @DisplayName("resolving in favour of client sets status to CANCELLED")
        void favourClient() {
            when(taskRepository.findById(5L)).thenReturn(Optional.of(disputedTask));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ResolveDisputeRequest req = new ResolveDisputeRequest();
            req.setResolution(DisputeResolution.FAVOUR_CLIENT);
            req.setReason("Worker did not show up");

            ActionResponse res = adminService.resolveDispute(5L, req);

            assertThat(res.isSuccess()).isTrue();
            assertThat(disputedTask.getStatus()).isEqualTo(TaskStatus.CANCELLED);
            verify(communicationService, times(2))
                    .sendDisputeResolvedEmail(anyString(), anyString(), eq(5L), anyString());
        }

        @Test
        @DisplayName("resolving in favour of worker sets status to COMPLETED")
        void favourWorker() {
            when(taskRepository.findById(5L)).thenReturn(Optional.of(disputedTask));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ResolveDisputeRequest req = new ResolveDisputeRequest();
            req.setResolution(DisputeResolution.FAVOUR_WORKER);
            req.setReason("Work was completed properly");

            adminService.resolveDispute(5L, req);

            assertThat(disputedTask.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        }

        @Test
        @DisplayName("throws when task is not in DISPUTED state")
        void taskNotDisputed() {
            disputedTask.setStatus(TaskStatus.COMPLETED);
            when(taskRepository.findById(5L)).thenReturn(Optional.of(disputedTask));

            ResolveDisputeRequest req = new ResolveDisputeRequest();
            req.setResolution(DisputeResolution.FAVOUR_CLIENT);
            req.setReason("test");

            assertThatThrownBy(() -> adminService.resolveDispute(5L, req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("disputed");
        }
    }

    @Nested
    @DisplayName("deleteUser()")
    class DeleteUser {

        @Test
        @DisplayName("soft-deletes user by setting status to DELETED")
        void softDeletes() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(clientUser));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ActionResponse res = adminService.deleteUser(1L);

            assertThat(res.isSuccess()).isTrue();
            assertThat(clientUser.getStatus()).isEqualTo(Status.DELETED);
        }
    }

    @Nested
    @DisplayName("assignTask()")
    class AssignTask {

        @Test
        @DisplayName("manually assigns worker to SEARCHING task and sets CONFIRMED")
        void assignsWorker() {
            Task searchingTask = Task.builder()
                    .id(10L).client(clientUser).serviceType(ServiceType.PLUMBING)
                    .title("Fix pipe").description("d").address("a")
                    .scheduledDate(LocalDate.now()).scheduledStart(LocalTime.of(10, 0))
                    .status(TaskStatus.SEARCHING).build();

            when(taskRepository.findById(10L)).thenReturn(Optional.of(searchingTask));
            when(userRepository.findById(2L)).thenReturn(Optional.of(workerUser));
            when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            TaskResponse mockResponse = new TaskResponse();
            when(taskMapper.toResponse(any())).thenReturn(mockResponse);
            doNothing().when(workerSnapshotService).captureOnAssignment(anyLong(), anyLong());

            AssignTaskRequest req = new AssignTaskRequest();
            req.setWorkerId(2L);
            req.setAgreedPrice(BigDecimal.valueOf(250));

            adminService.assignTask(10L, req);

            assertThat(searchingTask.getWorker()).isEqualTo(workerUser);
            assertThat(searchingTask.getStatus()).isEqualTo(TaskStatus.CONFIRMED);
            assertThat(searchingTask.getAgreedPrice()).isEqualByComparingTo(BigDecimal.valueOf(250));
            verify(notificationService).notifyTaskAccepted(searchingTask);
            verify(workerSnapshotService).captureOnAssignment(2L, 10L);
        }

        @Test
        @DisplayName("throws when task is not SEARCHING or PENDING")
        void taskInWrongStatus() {
            disputedTask.setStatus(TaskStatus.COMPLETED);
            when(taskRepository.findById(5L)).thenReturn(Optional.of(disputedTask));

            AssignTaskRequest req = new AssignTaskRequest();
            req.setWorkerId(2L);

            assertThatThrownBy(() -> adminService.assignTask(5L, req))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("listUsers()")
    class ListUsers {

        @Test
        @DisplayName("lists all users with no filter")
        void listAllUsers() {
            Pageable pg = PageRequest.of(0, 10);
            Page<User> page = new PageImpl<>(List.of(clientUser, workerUser));
            when(userRepository.search(null, null, null, pg)).thenReturn(page);

            Page<UserAdminDto> res = adminService.listUsers(null, null, null, pg);
            assertThat(res.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("filters users by role CLIENT")
        void filterByRole() {
            Pageable pg = PageRequest.of(0, 10);
            Page<User> page = new PageImpl<>(List.of(clientUser));
            when(userRepository.search(Role.CLIENT, null, null, pg)).thenReturn(page);

            Page<UserAdminDto> res = adminService.listUsers("CLIENT", null, null, pg);
            assertThat(res.getTotalElements()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("getPendingVerifications() returns workers with unverified CIN")
    void getPendingVerifications() {
        Pageable pg = PageRequest.of(0, 10);
        Page<WorkerProfile> page = new PageImpl<>(List.of(workerProfile));
        when(workerProfileRepository.findByCinVerifiedFalseAndUserStatus(Status.PENDING, pg))
                .thenReturn(page);

        Page<WorkerVerificationDto> res = adminService.getPendingVerifications(pg);
        assertThat(res.getTotalElements()).isEqualTo(1);
        assertThat(res.getContent().get(0).getFirstName()).isEqualTo("Yassine");
    }
}