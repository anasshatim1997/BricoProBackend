package com.bricopro.integration;

import com.bricopro.bidding.entity.Bid;
import com.bricopro.bidding.repository.BidRepository;
import com.bricopro.task.entity.Task;
import com.bricopro.task.repository.TaskRepository;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.WorkerAvailability;
import com.bricopro.user.entity.WorkerProfile;
import com.bricopro.user.entity.WorkerService;
import com.bricopro.user.repository.UserRepository;
import com.bricopro.user.repository.WorkerAvailabilityRepository;
import com.bricopro.user.repository.WorkerProfileRepository;
import com.bricopro.user.repository.WorkerServiceRepository;
import com.bricopro.user.repository.WorkerSnapshotHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BiddingFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkerProfileRepository workerProfileRepository;

    @Autowired
    private WorkerAvailabilityRepository availabilityRepository;

    @Autowired
    private WorkerServiceRepository workerServiceRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private WorkerSnapshotHistoryRepository workerSnapshotHistoryRepository;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @BeforeEach
    void setUp() {
        bidRepository.deleteAllInBatch();
        availabilityRepository.deleteAllInBatch();
        workerServiceRepository.deleteAllInBatch();
        workerSnapshotHistoryRepository.deleteAllInBatch();
        taskRepository.deleteAllInBatch();
        workerProfileRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    private RequestPostProcessor as(User user) {
        return SecurityMockMvcRequestPostProcessors.user(user);
    }

    @Test
    void fullBiddingFlow() throws Exception {
        User client = userRepository.save(User.builder()
                .firstName("Client")
                .lastName("Test")
                .email("client@test.com")
                .role(User.Role.CLIENT)
                .status(User.Status.ACTIVE)
                .build());

        User worker1 = userRepository.save(User.builder()
                .firstName("Worker")
                .lastName("One")
                .email("worker1@test.com")
                .role(User.Role.WORKER)
                .status(User.Status.ACTIVE)
                .build());

        User worker2 = userRepository.save(User.builder()
                .firstName("Worker")
                .lastName("Two")
                .email("worker2@test.com")
                .role(User.Role.WORKER)
                .status(User.Status.ACTIVE)
                .build());

        WorkerProfile profile1 = workerProfileRepository.save(WorkerProfile.builder()
                .user(worker1)
                .latitude(10.0)
                .longitude(20.0)
                .reliabilityScore(80)
                .averageRating(BigDecimal.valueOf(4.5))
                .build());

        WorkerProfile profile2 = workerProfileRepository.save(WorkerProfile.builder()
                .user(worker2)
                .latitude(10.2)
                .longitude(20.2)
                .reliabilityScore(90)
                .averageRating(BigDecimal.valueOf(4.8))
                .build());

        workerServiceRepository.save(WorkerService.builder()
                .workerProfile(profile1)
                .serviceType(WorkerProfile.ServiceType.PLUMBING)
                .build());

        workerServiceRepository.save(WorkerService.builder()
                .workerProfile(profile2)
                .serviceType(WorkerProfile.ServiceType.PLUMBING)
                .build());

        availabilityRepository.save(WorkerAvailability.builder()
                .workerProfile(profile1)
                .date(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(18, 0))
                .status(WorkerAvailability.AvailabilityStatus.AVAILABLE)
                .build());

        availabilityRepository.save(WorkerAvailability.builder()
                .workerProfile(profile2)
                .date(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(18, 0))
                .status(WorkerAvailability.AvailabilityStatus.AVAILABLE)
                .build());

        Task task = Task.builder()
                .client(client)
                .serviceType(WorkerProfile.ServiceType.PLUMBING)
                .title("Fix pipe")
                .description("Broken pipe")
                .address("123 Main St")
                .latitude(10.1)
                .longitude(20.1)
                .scheduledDate(LocalDate.now().plusDays(1))
                .scheduledStart(LocalTime.of(9, 0))
                .scheduledEnd(LocalTime.of(17, 0))
                .biddingEnabled(true)
                .autoAssignEnabled(false)
                .status(Task.TaskStatus.SEARCHING)
                .build();

        Task savedTask = taskRepository.save(task);

        String createBidJson = "{\"taskId\":" + savedTask.getId() + ",\"amount\":150,\"message\":\"I can fix it\",\"estimatedDurationHours\":2}";

        mockMvc.perform(post("/api/v1/bids")
                        .with(as(worker1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBidJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(savedTask.getId()))
                .andExpect(jsonPath("$.workerId").value(worker1.getId()))
                .andExpect(jsonPath("$.amount").value(150));

        String createBidJson2 = "{\"taskId\":" + savedTask.getId() + ",\"amount\":160,\"message\":\"I can also fix it\",\"estimatedDurationHours\":2}";

        mockMvc.perform(post("/api/v1/bids")
                        .with(as(worker2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBidJson2))
                .andExpect(status().isOk());

        Bid bid = bidRepository.findByTaskIdAndWorkerId(savedTask.getId(), worker1.getId()).orElseThrow();
        assertThat(bid.getStatus()).isEqualTo(Bid.BidStatus.PENDING);

        mockMvc.perform(post("/api/v1/bids/" + bid.getId() + "/accept")
                        .with(as(client)))
                .andExpect(status().isOk());

        Task updatedTask = taskRepository.findById(savedTask.getId()).orElseThrow();
        assertThat(updatedTask.getWorker()).isNotNull();
        assertThat(updatedTask.getWorker().getId()).isEqualTo(worker1.getId());
        assertThat(updatedTask.getStatus()).isEqualTo(Task.TaskStatus.CONFIRMED);

        bid = bidRepository.findById(bid.getId()).orElseThrow();
        assertThat(bid.getStatus()).isEqualTo(Bid.BidStatus.ACCEPTED);

        boolean allOtherBidsRejected = !bidRepository.findByTaskIdAndStatus(savedTask.getId(), Bid.BidStatus.REJECTED).isEmpty();
        assertThat(allOtherBidsRejected).isTrue();
    }
}