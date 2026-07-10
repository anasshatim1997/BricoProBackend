package com.bricopro.task.controller;

import com.bricopro.task.dto.TaskDtos.*;
import com.bricopro.task.entity.Task.TaskStatus;
import com.bricopro.task.service.TaskService;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.WorkerProfile.ServiceType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks")
@SecurityRequirement(name = "bearerAuth")
public class TaskController {

    private final TaskService taskService;

    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error: check request body or query parameters", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden: your account role cannot perform this action", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Not found: the requested resource does not exist", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(
        summary = "Create a new service request",
        description = "Create. Validates the incoming request body, persists the new resource, and returns the created entity with its server-generated ID."
    )
    public ResponseEntity<TaskResponse> create(@AuthenticationPrincipal User user,
                                               @Valid @RequestBody CreateTaskRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(user, req));
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error: check request body or query parameters", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden: your account role cannot perform this action", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Not found: the requested resource does not exist", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/{id}")
    @Operation(
        summary = "Get task details",
        description = "Get. Returns a paginated list of matching resources. Use the query parameters to filter and control sort order."
    )
    public ResponseEntity<TaskResponse> get(@Parameter(name = "id", description = "Unique identifier of the target resource", required = true, in = ParameterIn.PATH) @PathVariable Long id) {
        return ResponseEntity.ok(taskService.get(id));
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error: check request body or query parameters", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden: your account role cannot perform this action", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Not found: the requested resource does not exist", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/mine/client")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(
        summary = "Get client's own tasks",
        description = "My Client Tasks. Returns a paginated list of matching resources. Use the query parameters to filter and control sort order."
    )
    public ResponseEntity<Page<TaskResponse>> myClientTasks(@AuthenticationPrincipal User user,
                                                             @Parameter(name = "status", description = "Query filter or pagination parameter", required = false, in = ParameterIn.QUERY) @RequestParam(required = false) TaskStatus status,
                                                             Pageable pageable) {
        return ResponseEntity.ok(taskService.getClientTasks(user.getId(), status, pageable));
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error: check request body or query parameters", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden: your account role cannot perform this action", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Not found: the requested resource does not exist", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/mine/worker")
    @PreAuthorize("hasRole('WORKER')")
    @Operation(
        summary = "Get worker's assigned tasks",
        description = "My Worker Tasks. Returns a paginated list of matching resources. Use the query parameters to filter and control sort order."
    )
    public ResponseEntity<Page<TaskResponse>> myWorkerTasks(@AuthenticationPrincipal User user,
                                                             @Parameter(name = "status", description = "Query filter or pagination parameter", required = false, in = ParameterIn.QUERY) @RequestParam(required = false) TaskStatus status,
                                                             Pageable pageable) {
        return ResponseEntity.ok(taskService.getWorkerTasks(user.getId(), status, pageable));
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error: check request body or query parameters", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden: your account role cannot perform this action", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Not found: the requested resource does not exist", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/available")
    @PreAuthorize("hasRole('WORKER')")
    @Operation(
        summary = "Browse available tasks for workers",
        description = "Available. Returns a paginated list of matching resources. Use the query parameters to filter and control sort order."
    )
    public ResponseEntity<Page<TaskResponse>> available(@Parameter(name = "serviceType", description = "Query filter or pagination parameter", required = false, in = ParameterIn.QUERY) @RequestParam(required = false) ServiceType serviceType,
                                                         Pageable pageable) {
        return ResponseEntity.ok(taskService.getAvailableTasks(serviceType, pageable));
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error: check request body or query parameters", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden: your account role cannot perform this action", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Not found: the requested resource does not exist", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('WORKER')")
    @Operation(
        summary = "Worker accepts an available task",
        description = "Accept. Validates the incoming request body, persists the new resource, and returns the created entity with its server-generated ID."
    )
    public ResponseEntity<TaskResponse> accept(@AuthenticationPrincipal User user,
                                               @Parameter(name = "id", description = "Unique identifier of the target resource", required = true, in = ParameterIn.PATH) @PathVariable Long id) {
        return ResponseEntity.ok(taskService.acceptTask(user, id));
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Updated successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error: check request body or query parameters", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden: your account role cannot perform this action", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Not found: the requested resource does not exist", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @PatchMapping("/{id}/status")
    @Operation(
        summary = "Update task status (start / complete / cancel)",
        description = "Update Status. Updates the resource identified by the path parameter. Only fields supplied in the request body are modified; others remain unchanged."
    )
    public ResponseEntity<TaskResponse> updateStatus(@AuthenticationPrincipal User user,
                                                      @Parameter(name = "id", description = "Unique identifier of the target resource", required = true, in = ParameterIn.PATH) @PathVariable Long id,
                                                      @Valid @RequestBody UpdateTaskStatusRequest req) {
        return ResponseEntity.ok(taskService.updateStatus(user, id, req));
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error: check request body or query parameters", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden: your account role cannot perform this action", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Not found: the requested resource does not exist", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping("/{id}/reviews")
    @Operation(
        summary = "Submit a review after task completion",
        description = "Review. Validates the incoming request body, persists the new resource, and returns the created entity with its server-generated ID."
    )
    public ResponseEntity<ReviewResponse> review(@AuthenticationPrincipal User user,
                                                  @Parameter(name = "id", description = "Unique identifier of the target resource", required = true, in = ParameterIn.PATH) @PathVariable Long id,
                                                  @Valid @RequestBody CreateReviewRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.submitReview(user, id, req));
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error: check request body or query parameters", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden: your account role cannot perform this action", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Not found: the requested resource does not exist", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/reviews/{userId}")
    @Operation(
        summary = "Get all reviews for a user",
        description = "Get Reviews. Returns a paginated list of matching resources. Use the query parameters to filter and control sort order."
    )
    public ResponseEntity<Page<ReviewResponse>> getReviews(@Parameter(name = "id", description = "Unique identifier of the target resource", required = true, in = ParameterIn.PATH) @PathVariable Long userId, Pageable pageable) {
        return ResponseEntity.ok(taskService.getReviews(userId, pageable));
    }
}
