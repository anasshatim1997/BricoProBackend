package com.bricopro.user.controller;

import com.bricopro.user.entity.WorkerPortfolio;
import com.bricopro.user.entity.WorkerProfile.ServiceType;
import com.bricopro.user.service.PortfolioService;
import com.bricopro.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/v1/portfolio")
@RequiredArgsConstructor
@Tag(name = "Worker Portfolio")
@SecurityRequirement(name = "bearerAuth")
public class PortfolioController {

    private final PortfolioService portfolioService;

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error: check request body or query parameters", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden: your account role cannot perform this action", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Not found: the requested resource does not exist", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/workers/{userId}")
    @Operation(
        summary = "Get public portfolio for a worker",
        description = "Get Portfolio. Returns a paginated list of matching resources. Use the query parameters to filter and control sort order."
    )
    public ResponseEntity<List<WorkerPortfolio>> getPortfolio(@Parameter(name = "id", description = "Unique identifier of the target resource", required = true, in = ParameterIn.PATH) @PathVariable Long userId) {
        return ResponseEntity.ok(portfolioService.getPortfolio(userId));
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error: check request body or query parameters", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden: your account role cannot perform this action", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Not found: the requested resource does not exist", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('WORKER')")
    @Operation(
        summary = "Add before/after photo to portfolio",
        description = "Add Photo. Validates the incoming request body, persists the new resource, and returns the created entity with its server-generated ID."
    )
    public ResponseEntity<WorkerPortfolio> addPhoto(
            @AuthenticationPrincipal User user,
            @Parameter(name = "file", description = "Query filter or pagination parameter", required = true, in = ParameterIn.QUERY) @RequestParam("file") MultipartFile file,
            @Parameter(name = "caption", description = "Query filter or pagination parameter", required = false, in = ParameterIn.QUERY) @RequestParam(required = false) String caption,
            @Parameter(name = "param", description = "Query filter or pagination parameter", required = false, in = ParameterIn.QUERY) @RequestParam(required = false) ServiceType serviceType) {
        return ResponseEntity.ok(portfolioService.addPhoto(user.getId(), file, caption, serviceType));
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error: check request body or query parameters", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden: your account role cannot perform this action", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Not found: the requested resource does not exist", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @DeleteMapping("/me/{photoId}")
    @PreAuthorize("hasRole('WORKER')")
    @Operation(
        summary = "Delete a portfolio photo",
        description = "Delete Photo. Permanently removes the specified resource. This operation cannot be undone."
    )
    public ResponseEntity<Void> deletePhoto(@AuthenticationPrincipal User user,
                                             @Parameter(name = "id", description = "Unique identifier of the target resource", required = true, in = ParameterIn.PATH) @PathVariable Long photoId) {
        portfolioService.deletePhoto(user.getId(), photoId);
        return ResponseEntity.noContent().build();
    }
}
