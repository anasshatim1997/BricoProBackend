package com.bricopro.user.service;

import com.bricopro.user.dto.UserDtos.*;
import com.bricopro.user.entity.*;
import com.bricopro.user.entity.WorkerProfile.ServiceType;
import com.bricopro.user.mapper.UserMapper;
import com.bricopro.user.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "User Service", description = "Business logic for User Service")
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository              userRepository;
    private final WorkerProfileRepository     workerProfileRepository;
    private final WorkerServiceRepository     workerServiceRepository;   
    private final ClientProfileRepository     clientProfileRepository;
    private final WorkerAvailabilityRepository availabilityRepository;
    private final UserMapper mapper;

    /**
     * Get User.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public UserSummary getUser(Long userId) {
        return mapper.toSummary(userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found")));
    }

    @Transactional
    /**
     * Update User.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public UserSummary updateUser(Long userId, UpdateUserRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (req.getFirstName() != null) user.setFirstName(req.getFirstName());
        if (req.getLastName()  != null) user.setLastName(req.getLastName());
        if (req.getAvatarUrl() != null) user.setAvatarUrl(req.getAvatarUrl());
        return mapper.toSummary(userRepository.save(user));
    }

    /**
     * Get Worker Profile.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public WorkerProfileResponse getWorkerProfile(Long userId) {
        return mapper.toWorkerResponse(workerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Worker profile not found")));
    }

    @Transactional
    /**
     * Update Worker Profile.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public WorkerProfileResponse updateWorkerProfile(Long userId, UpdateWorkerProfileRequest req) {
        WorkerProfile profile = workerProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("User not found"));
                    WorkerProfile p = new WorkerProfile();
                    p.setUser(user);
                    return p;
                });

        if (req.getBio()                  != null) profile.setBio(req.getBio());
        if (req.getInterventionRadiusKm() != null) profile.setInterventionRadiusKm(req.getInterventionRadiusKm());
        if (req.getCity()                 != null) profile.setCity(req.getCity());
        if (req.getLatitude()             != null) profile.setLatitude(req.getLatitude());
        if (req.getLongitude()            != null) profile.setLongitude(req.getLongitude());
        if (req.getBankAccount()          != null) profile.setBankAccount(req.getBankAccount());

        WorkerProfile saved = workerProfileRepository.save(profile);

if (req.getServices() != null && !req.getServices().isEmpty()) {
            workerServiceRepository.deleteAllByWorkerProfileId(saved.getId());

            List<WorkerService> newServices = req.getServices().stream()
                    .map(dto -> WorkerService.builder()
                            .workerProfile(saved)
                            .serviceType(dto.getServiceType())
                            .hourlyRate(dto.getHourlyRate())
                            .build())
                    .collect(Collectors.toList());

            workerServiceRepository.saveAll(newServices);
        }

        return mapper.toWorkerResponse(workerProfileRepository.findByUserId(userId)
                .orElseThrow());
    }

    /**
     * Search Workers.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public Page<WorkerProfileResponse> searchWorkers(ServiceType serviceType, String city, Pageable pageable) {
        return workerProfileRepository.findByFilters(serviceType, city, pageable)
                .map(mapper::toWorkerResponse);
    }

    /**
     * Get Client Profile.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public ClientProfileResponse getClientProfile(Long userId) {
        return mapper.toClientResponse(clientProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Client profile not found")));
    }

    @Transactional
    /**
     * Update Client Profile.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public ClientProfileResponse updateClientProfile(Long userId, UpdateClientProfileRequest req) {
        ClientProfile profile = clientProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("User not found"));
                    ClientProfile p = new ClientProfile();
                    p.setUser(user);
                    return p;
                });

        if (req.getCompanyName()      != null) profile.setCompanyName(req.getCompanyName());
        if (req.getCity()             != null) profile.setCity(req.getCity());
        if (req.getDefaultAddress()   != null) profile.setDefaultAddress(req.getDefaultAddress());
        if (req.getDefaultLatitude()  != null) profile.setDefaultLatitude(req.getDefaultLatitude());
        if (req.getDefaultLongitude() != null) profile.setDefaultLongitude(req.getDefaultLongitude());

        return mapper.toClientResponse(clientProfileRepository.save(profile));
    }

    /**
     * Get Availability.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public List<AvailabilityResponse> getAvailability(Long userId, LocalDate from, LocalDate to) {
        WorkerProfile profile = workerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Worker profile not found"));
        return mapper.toAvailabilityList(
                availabilityRepository.findByWorkerProfileIdAndDateBetween(profile.getId(), from, to));
    }

    @Transactional
    /**
     * Set Availability.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public AvailabilityResponse setAvailability(Long userId, AvailabilityRequest req) {
        WorkerProfile profile = workerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Worker profile not found"));

        WorkerAvailability avail = availabilityRepository
                .findByWorkerProfileIdAndDate(profile.getId(), req.getDate())
                .orElseGet(() -> {
                    WorkerAvailability a = new WorkerAvailability();
                    a.setWorkerProfile(profile);
                    a.setDate(req.getDate());
                    return a;
                });

        avail.setStatus(req.getStatus());
        avail.setStartTime(req.getStartTime());
        avail.setEndTime(req.getEndTime());
        return mapper.toAvailabilityResponse(availabilityRepository.save(avail));
    }
}
