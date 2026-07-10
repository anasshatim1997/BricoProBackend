package com.bricopro.subscription.controller;

import com.bricopro.subscription.dto.CreateCampaignRequest;
import com.bricopro.subscription.entity.SponsoredWorker;
import com.bricopro.subscription.service.SponsoredVisibilityService;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.WorkerProfile.ServiceType;
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
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sponsored")
@RequiredArgsConstructor
@Tag(name = "Sponsored Visibility")
@SecurityRequirement(name = "bearerAuth")
public class SponsoredController {

    private final SponsoredVisibilityService service;

    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "Unauthorised", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping("/campaign")
    @PreAuthorize("hasRole('WORKER')")
    @Operation(summary = "Launch a sponsored visibility campaign")
    public ResponseEntity<SponsoredWorker> create(@AuthenticationPrincipal User user,
                                                   @RequestBody CreateCampaignRequest req) {
        return ResponseEntity.ok(service.createCampaign(user.getId(), req));
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorised", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping
    @Operation(summary = "Get sponsored workers for a service/city (client view)")
    public ResponseEntity<List<SponsoredWorker>> getSponsored(
            @Parameter(name = "serviceType", required = false, in = ParameterIn.QUERY) @RequestParam(required = false) ServiceType serviceType,
            @Parameter(name = "city", required = false, in = ParameterIn.QUERY) @RequestParam(required = false) String city) {
        return ResponseEntity.ok(service.getSponsored(serviceType, city));
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Click recorded"),
        @ApiResponse(responseCode = "401", description = "Unauthorised", content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping("/{id}/click")
    @Operation(
        summary = "Record a click on a sponsored worker (called by frontend)",
        description = "Deduplicated per (campaign, viewer) — a given account can only count once per campaign, regardless of how many times this is called."
    )
    public ResponseEntity<Void> click(@AuthenticationPrincipal User user,
                                       @Parameter(name = "id", required = true, in = ParameterIn.PATH) @PathVariable Long id) {
        service.recordClick(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorised", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/stats")
    @PreAuthorize("hasRole('WORKER')")
    @Operation(summary = "Get my campaign statistics")
    public ResponseEntity<Map<String, Object>> stats(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(service.getCampaignStats(user.getId()));
    }
}
