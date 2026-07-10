package com.bricopro.user.controller;

import com.bricopro.user.dto.UserDtos.*;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.WorkerProfile.ServiceType;
import com.bricopro.user.service.UserService;
import com.bricopro.verification.CinVerificationService;
import com.bricopro.verification.dto.CinVerificationDtos.CinVerificationResponse;
import com.bricopro.verification.dto.CinVerificationDtos.WorkerProfileDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;
    private final CinVerificationService cinVerificationService;

    @GetMapping("/me")
    public ResponseEntity<UserSummary> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.getUser(user.getId()));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserSummary> updateMe(@AuthenticationPrincipal User user,
                                                @Valid @RequestBody UpdateUserRequest req) {
        return ResponseEntity.ok(userService.updateUser(user.getId(), req));
    }

    @GetMapping("/workers")
    public ResponseEntity<Page<WorkerProfileResponse>> searchWorkers(
            @RequestParam(required = false) ServiceType serviceType,
            @RequestParam(required = false) String city,
            Pageable pageable) {
        return ResponseEntity.ok(userService.searchWorkers(serviceType, city, pageable));
    }

    @GetMapping("/workers/{userId}")
    public ResponseEntity<WorkerProfileResponse> getWorkerProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getWorkerProfile(userId));
    }

    @GetMapping("/me/worker-profile")
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<WorkerProfileDetailResponse> getMyWorkerProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(cinVerificationService.getMyProfile(user.getId()));
    }

    @PutMapping("/me/worker-profile")
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<WorkerProfileResponse> updateWorkerProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateWorkerProfileRequest req) {
        return ResponseEntity.ok(userService.updateWorkerProfile(user.getId(), req));
    }

    @PostMapping(value = "/me/worker-profile/cin-verification", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<CinVerificationResponse> submitCinVerification(
            @AuthenticationPrincipal User user,
            @RequestParam("cin") MultipartFile file) {
        return ResponseEntity.ok(cinVerificationService.submitCin(user.getId(), file));
    }

    @PutMapping("/me/client-profile")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ClientProfileResponse> updateClientProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdateClientProfileRequest req) {
        return ResponseEntity.ok(userService.updateClientProfile(user.getId(), req));
    }

    @GetMapping("/me/availability")
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<List<AvailabilityResponse>> getAvailability(
            @AuthenticationPrincipal User user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(userService.getAvailability(user.getId(), from, to));
    }

    @PutMapping("/me/availability")
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<AvailabilityResponse> setAvailability(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AvailabilityRequest req) {
        return ResponseEntity.ok(userService.setAvailability(user.getId(), req));
    }


}