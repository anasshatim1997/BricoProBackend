package com.bricopro.analytics.controller;

import com.bricopro.analytics.WorkerPerformanceService;
import com.bricopro.analytics.service.ChurnPredictionService;
import com.bricopro.analytics.service.ClientLtvService;
import com.bricopro.analytics.service.DemandHeatmapService;
import com.bricopro.analytics.service.EarningsForecastService;
import com.bricopro.analytics.service.FraudDetectionService;
import com.bricopro.analytics.service.PlatformRevenueService;
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
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Advanced Analytics")
@SecurityRequirement(name = "bearerAuth")
public class AdvancedAnalyticsController {

    private final DemandHeatmapService     heatmapService;
    private final ClientLtvService         ltvService;
    private final EarningsForecastService  forecastService;
    private final PlatformRevenueService   revenueService;
    private final FraudDetectionService    fraudService;
    private final ChurnPredictionService   churnService;
    private final WorkerPerformanceService performanceService;

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorised", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/heatmap")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Demand heatmap by service type")
    public ResponseEntity<List<DemandHeatmapService.HeatmapPoint>> heatmap() {
        return ResponseEntity.ok(heatmapService.getHeatmap());
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorised", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/clients/{clientId}/ltv")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Client lifetime-value report")
    public ResponseEntity<ClientLtvService.ClientLtvReport> clientLtv(
            @Parameter(name = "clientId", required = true, in = ParameterIn.PATH) @PathVariable Long clientId) {
        return ResponseEntity.ok(ltvService.getClientLtv(clientId));
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorised", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/forecast")
    @PreAuthorize("hasRole('WORKER')")
    @Operation(summary = "My earnings forecast")
    public ResponseEntity<EarningsForecastService.ForecastReport> forecast(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(forecastService.forecast(user.getId()));
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorised", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/revenue")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Platform revenue breakdown report")
    public ResponseEntity<PlatformRevenueService.PlatformRevenueReport> platformRevenue(
            @Parameter(name = "year", required = false, in = ParameterIn.QUERY) @RequestParam(defaultValue = "2026") int year) {
        return ResponseEntity.ok(revenueService.getReport(year));
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorised", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/fraud/{workerId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Fraud-signal report for a worker")
    public ResponseEntity<FraudDetectionService.FraudReport> fraud(
            @Parameter(name = "workerId", required = true, in = ParameterIn.PATH) @PathVariable Long workerId) {
        return ResponseEntity.ok(fraudService.analyzeWorker(workerId));
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorised", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/churn/{clientId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Churn-risk prediction report for a client")
    public ResponseEntity<ChurnPredictionService.ChurnReport> churn(
            @Parameter(name = "clientId", required = true, in = ParameterIn.PATH) @PathVariable Long clientId) {
        return ResponseEntity.ok(churnService.predictClientChurn(clientId));
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorised", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/workers/leaderboard")
    @Operation(summary = "Top workers by performance score")
    public ResponseEntity<List<WorkerPerformanceService.PerformanceReport>> leaderboard(
            @Parameter(name = "limit", required = false, in = ParameterIn.QUERY) @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(performanceService.getLeaderboard(limit));
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorised", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/workers/performance")
    @PreAuthorize("hasRole('WORKER')")
    @Operation(summary = "My performance report and tier")
    public ResponseEntity<WorkerPerformanceService.PerformanceReport> myPerformance(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(performanceService.getReport(user.getId()));
    }
}
