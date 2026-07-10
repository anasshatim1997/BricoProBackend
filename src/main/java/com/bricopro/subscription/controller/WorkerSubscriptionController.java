package com.bricopro.subscription.controller;

import com.bricopro.subscription.entity.WorkerSubscription;
import com.bricopro.subscription.service.WorkerSubscriptionService;
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

import java.util.Map;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Worker Subscriptions")
@SecurityRequirement(name = "bearerAuth")
public class WorkerSubscriptionController {

    private final WorkerSubscriptionService service;

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/plans")
    @Operation(summary = "Get available subscription plans and prices")
    public ResponseEntity<Map<String, Object>> plans() {
        return ResponseEntity.ok(service.getPlansInfo());
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorised", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/me")
    @PreAuthorize("hasRole('WORKER')")
    @Operation(summary = "Get my current subscription plan")
    public ResponseEntity<WorkerSubscription> myPlan(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.getCurrentPlan(user.getId()));
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "Unauthorised", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping("/upgrade")
    @PreAuthorize("hasRole('WORKER')")
    @Operation(summary = "Upgrade to Premium or Enterprise plan")
    public ResponseEntity<WorkerSubscription> upgrade(@AuthenticationPrincipal User user,
                                                       @Parameter(name = "plan", required = true, in = ParameterIn.QUERY) @RequestParam WorkerSubscription.Plan plan) {
        if (plan == WorkerSubscription.Plan.FREE)
            throw new IllegalArgumentException("Cannot upgrade to FREE plan");
        return ResponseEntity.ok(service.upgrade(user.getId(), plan));
    }
}
