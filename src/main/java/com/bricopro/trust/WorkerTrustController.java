package com.bricopro.trust;

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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Worker Trust")
@SecurityRequirement(name = "bearerAuth")
public class WorkerTrustController {

    private final WorkerTrustService service;

    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "Unauthorised", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping("/workers/{workerId}/recommend")
    @Operation(summary = "Recommend a worker to your network")
    public ResponseEntity<WorkerRecommendation> recommend(
            @AuthenticationPrincipal User user,
            @Parameter(name = "workerId", required = true, in = ParameterIn.PATH) @PathVariable Long workerId,
            @RequestParam(required = false) String note) {
        return ResponseEntity.ok(service.recommend(user.getId(), workerId, note));
    }

    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorised", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/workers/{workerId}/trust")
    @Operation(summary = "Get worker trust score and network recommendations")
    public ResponseEntity<Map<String, Object>> trustScore(
            @AuthenticationPrincipal User user,
            @Parameter(name = "workerId", required = true, in = ParameterIn.PATH) @PathVariable Long workerId) {
        return ResponseEntity.ok(service.getWorkerTrustScore(workerId, user.getId()));
    }
}
