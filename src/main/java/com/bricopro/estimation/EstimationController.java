package com.bricopro.estimation;

import com.bricopro.estimation.PriceEstimationService.EstimationRequest;
import com.bricopro.estimation.PriceEstimationService.EstimationResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/v1/estimate")
@RequiredArgsConstructor
@Tag(name = "Price Estimation")
public class EstimationController {

    private final PriceEstimationService estimationService;

    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error: check request body or query parameters", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Not found: the requested resource does not exist", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping
    @Operation(
        summary = "Get instant price estimate before posting a task",
        description = "Estimate. Validates the incoming request body, persists the new resource, and returns the created entity with its server-generated ID."
    )
    public ResponseEntity<EstimationResult> estimate(@Valid @RequestBody EstimationRequest req) {
        return ResponseEntity.ok(estimationService.estimate(req));
    }
}
