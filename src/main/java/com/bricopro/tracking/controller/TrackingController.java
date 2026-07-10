package com.bricopro.tracking.controller;

import com.bricopro.tracking.dto.LocationUpdateRequest;
import com.bricopro.tracking.entity.WorkerLocation;
import com.bricopro.tracking.service.WorkerTrackingService;
import com.bricopro.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/tracking")
@RequiredArgsConstructor
@Tag(name = "GPS Tracking")
@SecurityRequirement(name = "bearerAuth")
public class TrackingController {

    private final WorkerTrackingService trackingService;

    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Updated successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden: your account role cannot perform this action", content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping("/location")
    @PreAuthorize("hasRole('WORKER')")
    @Operation(summary = "Worker pushes GPS location — broadcasts to client in real-time")
    public ResponseEntity<Void> updateLocation(@AuthenticationPrincipal User user,
                                                @RequestBody LocationUpdateRequest req) {
        trackingService.updateLocation(user.getId(), req);
        return ResponseEntity.noContent().build();
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden: you don't have an active task with this worker", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Not found: no location reported yet for this worker", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/workers/{workerId}")
    @Operation(
        summary = "Get a worker's last known location",
        description = "Only accessible to the worker themselves, or a client with a CONFIRMED/STARTED task with that worker."
    )
    public ResponseEntity<WorkerLocation> getWorkerLocation(
            @AuthenticationPrincipal User user,
            @Parameter(name = "workerId", required = true, in = ParameterIn.PATH) @PathVariable Long workerId) {
        try {
            return trackingService.getLocation(workerId, user.getId())
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (SecurityException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        }
    }
}
