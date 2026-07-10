package com.bricopro.booking.controller;

import com.bricopro.booking.dto.CreateGroupBookingRequest;
import com.bricopro.booking.entity.GroupBooking;
import com.bricopro.booking.entity.GroupBookingWorker;
import com.bricopro.booking.service.GroupBookingService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/group-bookings")
@RequiredArgsConstructor
@Tag(name = "Group Booking")
@SecurityRequirement(name = "bearerAuth")
public class GroupBookingController {

    private final GroupBookingService service;

    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "Unauthorised", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Create a group booking requiring multiple workers")
    public ResponseEntity<GroupBooking> create(@AuthenticationPrincipal User user,
                                                @RequestBody CreateGroupBookingRequest req) {
        return ResponseEntity.ok(service.create(user.getId(), req));
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorised", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/open")
    @PreAuthorize("hasRole('WORKER')")
    @Operation(summary = "Browse open group bookings available to join")
    public ResponseEntity<List<GroupBooking>> open() {
        return ResponseEntity.ok(service.getOpen());
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Joined successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "Unauthorised", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping("/{id}/join")
    @PreAuthorize("hasRole('WORKER')")
    @Operation(summary = "Worker joins a group booking")
    public ResponseEntity<GroupBookingWorker> join(@AuthenticationPrincipal User user,
                                                    @Parameter(name = "id", required = true, in = ParameterIn.PATH) @PathVariable Long id) {
        return ResponseEntity.ok(service.workerJoin(user.getId(), id));
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorised", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/mine")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Get my group bookings")
    public ResponseEntity<List<GroupBooking>> mine(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.getClientBookings(user.getId()));
    }
}
