package com.bricopro.user;

import com.bricopro.user.dto.UserDtos.*;
import com.bricopro.user.entity.*;
import com.bricopro.user.entity.User.Role;
import com.bricopro.user.entity.User.Status;
import com.bricopro.user.entity.WorkerAvailability.AvailabilityStatus;
import com.bricopro.user.entity.WorkerProfile.ServiceType;
import com.bricopro.user.mapper.UserMapper;
import com.bricopro.user.repository.*;
import com.bricopro.user.service.UserService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock WorkerProfileRepository workerProfileRepository;
    @Mock WorkerServiceRepository workerServiceRepository;
    @Mock ClientProfileRepository clientProfileRepository;
    @Mock WorkerAvailabilityRepository availabilityRepository;
    @Mock UserMapper mapper;

    @InjectMocks UserService userService;

    private User workerUser;
    private User clientUser;
    private WorkerProfile workerProfile;
    private ClientProfile clientProfile;

    @BeforeEach
    void setup() {
        workerUser = User.builder().id(2L).firstName("Driss").lastName("Benomar")
                .email("driss@test.ma").avatarUrl("http://img/d.jpg")
                .role(Role.WORKER).status(Status.ACTIVE).isVerified(true).build();

        clientUser = User.builder().id(1L).firstName("Samira").lastName("Lahlou")
                .email("samira@test.ma").role(Role.CLIENT).status(Status.ACTIVE).build();

        workerProfile = WorkerProfile.builder()
                .id(1L).user(workerUser).bio("Expert plumber")
                .averageRating(BigDecimal.valueOf(4.5)).totalReviews(20)
                .totalMissions(35).interventionRadiusKm(20)
                .city("Casablanca").latitude(33.5731).longitude(-7.5898)
                .isPremium(false).cinVerified(true).build();

        clientProfile = ClientProfile.builder()
                .id(1L).user(clientUser).city("Rabat")
                .defaultAddress("10 Av Mohammed V, Rabat").build();
    }

    // ─── GET USER ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getUser()")
    class GetUser {

        @Test
        @DisplayName("returns UserSummary for existing user")
        void returnsUser() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(clientUser));
            UserSummary summary = new UserSummary();
            summary.setId(1L);
            when(mapper.toSummary(clientUser)).thenReturn(summary);

            UserSummary res = userService.getUser(1L);
            assertThat(res.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("throws when user not found")
        void userNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> userService.getUser(99L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ─── UPDATE USER ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateUser()")
    class UpdateUser {

        @Test
        @DisplayName("updates firstName only when provided")
        void updatesFirstName() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(clientUser));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toSummary(any())).thenReturn(new UserSummary());

            UpdateUserRequest req = new UpdateUserRequest();
            req.setFirstName("Fatima");

            userService.updateUser(1L, req);

            assertThat(clientUser.getFirstName()).isEqualTo("Fatima");
            assertThat(clientUser.getLastName()).isEqualTo("Lahlou"); // unchanged
        }

        @Test
        @DisplayName("updates avatarUrl when provided")
        void updatesAvatarUrl() {
            when(userRepository.findById(2L)).thenReturn(Optional.of(workerUser));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toSummary(any())).thenReturn(new UserSummary());

            UpdateUserRequest req = new UpdateUserRequest();
            req.setAvatarUrl("http://new.img/avatar.jpg");

            userService.updateUser(2L, req);

            assertThat(workerUser.getAvatarUrl()).isEqualTo("http://new.img/avatar.jpg");
        }

        @Test
        @DisplayName("null fields in request are skipped")
        void nullFieldsSkipped() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(clientUser));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toSummary(any())).thenReturn(new UserSummary());

            UpdateUserRequest req = new UpdateUserRequest(); // all nulls

            userService.updateUser(1L, req);

            assertThat(clientUser.getFirstName()).isEqualTo("Samira"); // unchanged
        }
    }

    // ─── UPDATE WORKER PROFILE ────────────────────────────────────────────────

    @Nested
    @DisplayName("updateWorkerProfile()")
    class UpdateWorkerProfile {

        @Test
        @DisplayName("updates bio, city, and location")
        void updatesBioAndLocation() {
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(workerProfile));
            when(workerProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            // re-fetch after save
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(workerProfile));
            WorkerProfileResponse mockResponse = new WorkerProfileResponse();
            when(mapper.toWorkerResponse(any())).thenReturn(mockResponse);

            UpdateWorkerProfileRequest req = new UpdateWorkerProfileRequest();
            req.setBio("Specialist in plumbing and electrical works");
            req.setCity("Rabat");
            req.setLatitude(34.020882);
            req.setLongitude(-6.84165);

            userService.updateWorkerProfile(2L, req);

            assertThat(workerProfile.getBio()).isEqualTo("Specialist in plumbing and electrical works");
            assertThat(workerProfile.getCity()).isEqualTo("Rabat");
            assertThat(workerProfile.getLatitude()).isEqualTo(34.020882);
        }

        @Test
        @DisplayName("updates services list by deleting old and inserting new")
        void updatesServices() {
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(workerProfile));
            when(workerProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(workerProfile));
            when(mapper.toWorkerResponse(any())).thenReturn(new WorkerProfileResponse());

            WorkerServiceDto service = new WorkerServiceDto();
            service.setServiceType(ServiceType.PLUMBING);
            service.setHourlyRate(BigDecimal.valueOf(80));

            UpdateWorkerProfileRequest req = new UpdateWorkerProfileRequest();
            req.setServices(List.of(service));

            userService.updateWorkerProfile(2L, req);

            verify(workerServiceRepository).deleteAllByWorkerProfileId(1L);
            verify(workerServiceRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("creates new worker profile when it does not exist")
        void createsProfileIfMissing() {
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.empty());
            when(userRepository.findById(2L)).thenReturn(Optional.of(workerUser));
            when(workerProfileRepository.save(any())).thenAnswer(inv -> {
                WorkerProfile p = inv.getArgument(0);
                p = WorkerProfile.builder().id(99L).user(workerUser).bio(p.getBio()).build();
                return p;
            });
            when(workerProfileRepository.findByUserId(2L))
                    .thenReturn(Optional.of(workerProfile)); // second call returns saved
            when(mapper.toWorkerResponse(any())).thenReturn(new WorkerProfileResponse());

            UpdateWorkerProfileRequest req = new UpdateWorkerProfileRequest();
            req.setBio("New bio");

            userService.updateWorkerProfile(2L, req);

            verify(workerProfileRepository, atLeast(1)).save(any());
        }
    }

    // ─── UPDATE CLIENT PROFILE ────────────────────────────────────────────────

    @Nested
    @DisplayName("updateClientProfile()")
    class UpdateClientProfile {

        @Test
        @DisplayName("updates city and default address")
        void updatesCityAndAddress() {
            when(clientProfileRepository.findByUserId(1L)).thenReturn(Optional.of(clientProfile));
            when(clientProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toClientResponse(any())).thenReturn(new ClientProfileResponse());

            UpdateClientProfileRequest req = new UpdateClientProfileRequest();
            req.setCity("Casablanca");
            req.setDefaultAddress("5 Bd Anfa, Casablanca");

            userService.updateClientProfile(1L, req);

            assertThat(clientProfile.getCity()).isEqualTo("Casablanca");
            assertThat(clientProfile.getDefaultAddress()).isEqualTo("5 Bd Anfa, Casablanca");
        }

        @Test
        @DisplayName("creates new client profile when it does not exist")
        void createsProfileIfMissing() {
            when(clientProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());
            when(userRepository.findById(1L)).thenReturn(Optional.of(clientUser));
            when(clientProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toClientResponse(any())).thenReturn(new ClientProfileResponse());

            UpdateClientProfileRequest req = new UpdateClientProfileRequest();
            req.setCompanyName("ACME Corp");

            userService.updateClientProfile(1L, req);
            verify(clientProfileRepository).save(any());
        }
    }

    // ─── SEARCH WORKERS ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("searchWorkers()")
    class SearchWorkers {

        @Test
        @DisplayName("returns paginated worker profiles filtered by service and city")
        void searchByServiceAndCity() {
            PageRequest pg = PageRequest.of(0, 10);
            Page<WorkerProfile> page = new PageImpl<>(List.of(workerProfile));
            when(workerProfileRepository.findByFilters(ServiceType.PLUMBING, "Casablanca", pg))
                    .thenReturn(page);
            when(mapper.toWorkerResponse(any())).thenReturn(new WorkerProfileResponse());

            Page<WorkerProfileResponse> res = userService.searchWorkers(ServiceType.PLUMBING, "Casablanca", pg);
            assertThat(res.getTotalElements()).isEqualTo(1);
        }
    }

    // ─── AVAILABILITY ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("setAvailability() / getAvailability()")
    class Availability {

        @Test
        @DisplayName("creates new availability slot for a date")
        void setsAvailability() {
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(workerProfile));
            when(availabilityRepository.findByWorkerProfileIdAndDate(1L, LocalDate.of(2025, 7, 10)))
                    .thenReturn(Optional.empty());
            when(availabilityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toAvailabilityResponse(any())).thenReturn(new AvailabilityResponse());

            AvailabilityRequest req = new AvailabilityRequest();
            req.setDate(LocalDate.of(2025, 7, 10));
            req.setStatus(AvailabilityStatus.AVAILABLE);
            req.setStartTime(LocalTime.of(9, 0));
            req.setEndTime(LocalTime.of(17, 0));

            userService.setAvailability(2L, req);
            verify(availabilityRepository).save(argThat(a ->
                    a.getStatus() == AvailabilityStatus.AVAILABLE));
        }

        @Test
        @DisplayName("updates existing availability slot for a date")
        void updatesExistingSlot() {
            WorkerAvailability existing = WorkerAvailability.builder()
                    .id(1L).workerProfile(workerProfile)
                    .date(LocalDate.of(2025, 7, 10))
                    .status(AvailabilityStatus.AVAILABLE).build();

            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(workerProfile));
            when(availabilityRepository.findByWorkerProfileIdAndDate(1L, LocalDate.of(2025, 7, 10)))
                    .thenReturn(Optional.of(existing));
            when(availabilityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toAvailabilityResponse(any())).thenReturn(new AvailabilityResponse());

            AvailabilityRequest req = new AvailabilityRequest();
            req.setDate(LocalDate.of(2025, 7, 10));
            req.setStatus(AvailabilityStatus.BUSY);
            req.setStartTime(LocalTime.of(8, 0));
            req.setEndTime(LocalTime.of(12, 0));

            userService.setAvailability(2L, req);

            assertThat(existing.getStatus()).isEqualTo(AvailabilityStatus.BUSY);
        }

        @Test
        @DisplayName("getAvailability returns list of slots in date range")
        void getAvailabilityRange() {
            WorkerAvailability slot = WorkerAvailability.builder()
                    .id(1L).workerProfile(workerProfile)
                    .date(LocalDate.of(2025, 7, 10))
                    .status(AvailabilityStatus.AVAILABLE).build();

            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(workerProfile));
            when(availabilityRepository.findByWorkerProfileIdAndDateBetween(
                    1L, LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 31)))
                    .thenReturn(List.of(slot));
            AvailabilityResponse mockResp = new AvailabilityResponse();
            when(mapper.toAvailabilityList(anyList())).thenReturn(List.of(mockResp));

            List<AvailabilityResponse> res = userService.getAvailability(
                    2L, LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 31));

            assertThat(res).hasSize(1);
        }
    }
}
