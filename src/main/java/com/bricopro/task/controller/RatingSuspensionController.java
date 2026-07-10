package com.bricopro.task.controller;

import com.bricopro.task.service.RatingSuspensionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/admin/workers")
@RequiredArgsConstructor
public class RatingSuspensionController {

    private final RatingSuspensionService ratingSuspensionService;

    @Operation(
            summary = "Evaluate worker rating",
            description = "Checks a worker's average rating across all reviews. " +
                    "If it falls below 3.0 with at least 10 reviews, the account is automatically suspended.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Evaluation completed"),
                    @ApiResponse(responseCode = "404", description = "Worker profile not found"),
                    @ApiResponse(responseCode = "403", description = "Access denied")
            }
    )
    @PostMapping("/{workerId}/evaluate-rating")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> evaluateWorkerRating(
            @Parameter(description = "ID of the worker to evaluate", example = "1")
            @PathVariable Long workerId) {

        ratingSuspensionService.evaluateWorkerRating(workerId);
        return ResponseEntity.ok("Rating evaluation completed for worker " + workerId);
    }
}