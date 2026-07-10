package com.bricopro.admin.controller;

import com.bricopro.admin.service.AdminService;
import com.bricopro.admin.dto.AdminDtos.*;
import com.bricopro.task.dto.TaskDtos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminService adminService;

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error: check request body or query parameters", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden: your account role cannot perform this action", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Not found: the requested resource does not exist", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/workers/pending-verification")
    @Operation(
        summary = "List workers awaiting CIN verification",
        description = "Pending Verifications. Returns a paginated list of matching resources. Use the query parameters to filter and control sort order."
    )
    public ResponseEntity<Page<WorkerVerificationDto>> pendingVerifications(Pageable pageable) {
        return ResponseEntity.ok(adminService.getPendingVerifications(pageable));
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error: check request body or query parameters", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden: your account role cannot perform this action", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Not found: the requested resource does not exist", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping("/workers/{userId}/verify")
    @Operation(
        summary = "Approve worker CIN and activate account",
        description = "Verify Worker. Validates the incoming request body, persists the new resource, and returns the created entity with its server-generated ID."
    )
    public ResponseEntity<ActionResponse> verifyWorker(@Parameter(name = "userId", description = "Unique identifier of the target resource", required = true, in = ParameterIn.PATH) @PathVariable Long userId,
                                                        @Valid @RequestBody VerifyWorkerRequest req) {
        return ResponseEntity.ok(adminService.verifyWorker(userId, req));
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error: check request body or query parameters", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden: your account role cannot perform this action", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Not found: the requested resource does not exist", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping("/workers/{userId}/reject")
    @Operation(
        summary = "Reject worker CIN and notify them",
        description = "Reject Worker. Validates the incoming request body, persists the new resource, and returns the created entity with its server-generated ID."
    )
    public ResponseEntity<ActionResponse> rejectWorker(@Parameter(name = "userId", description = "Unique identifier of the target resource", required = true, in = ParameterIn.PATH) @PathVariable Long userId,
                                                        @Valid @RequestBody RejectRequest req) {
        return ResponseEntity.ok(adminService.rejectWorker(userId, req));
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error: check request body or query parameters", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden: your account role cannot perform this action", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Not found: the requested resource does not exist", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping("/users/{userId}/suspend")
    @Operation(
        summary = "Suspend a user account",
        description = "Suspend. Validates the incoming request body, persists the new resource, and returns the created entity with its server-generated ID."
    )
    public ResponseEntity<ActionResponse> suspend(@Parameter(name = "userId", description = "Unique identifier of the target resource", required = true, in = ParameterIn.PATH) @PathVariable Long userId,
                                                   @Valid @RequestBody SuspendRequest req) {
        return ResponseEntity.ok(adminService.suspendUser(userId, req));
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error: check request body or query parameters", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden: your account role cannot perform this action", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Not found: the requested resource does not exist", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping("/users/{userId}/reactivate")
    @Operation(
        summary = "Reactivate a suspended account",
        description = "Reactivate. Validates the incoming request body, persists the new resource, and returns the created entity with its server-generated ID."
    )
    public ResponseEntity<ActionResponse> reactivate(@Parameter(name = "id", description = "Unique identifier of the target resource", required = true, in = ParameterIn.PATH) @PathVariable Long userId) {
        return ResponseEntity.ok(adminService.reactivateUser(userId));
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error: check request body or query parameters", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden: your account role cannot perform this action", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Not found: the requested resource does not exist", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/tasks/disputed")
    @Operation(
        summary = "List all disputed tasks",
        description = "Disputed Tasks. Returns a paginated list of matching resources. Use the query parameters to filter and control sort order."
    )
    public ResponseEntity<Page<DisputedTaskDto>> disputedTasks(Pageable pageable) {
        return ResponseEntity.ok(adminService.getDisputedTasks(pageable));
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error: check request body or query parameters", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden: your account role cannot perform this action", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Not found: the requested resource does not exist", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping("/tasks/{taskId}/resolve-dispute")
    @Operation(
        summary = "Resolve a disputed task — rule in favour of client or worker",
        description = "Resolve Dispute. Validates the incoming request body, persists the new resource, and returns the created entity with its server-generated ID."
    )
    public ResponseEntity<ActionResponse> resolveDispute(@Parameter(name = "taskId", description = "Unique identifier of the target resource", required = true, in = ParameterIn.PATH) @PathVariable Long taskId,
                                                          @Valid @RequestBody ResolveDisputeRequest req) {
        return ResponseEntity.ok(adminService.resolveDispute(taskId, req));
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden: your account role cannot perform this action", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Not found: the requested resource does not exist", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping("/payments/{paymentId}/resolve-dispute")
    @Operation(
        summary = "Resolve a disputed cash payment — rule in favour of client or worker",
        description = "Resolve Payment Dispute. Validates the incoming request body and applies the admin's ruling to the disputed payment."
    )
    public ResponseEntity<ActionResponse> resolvePaymentDispute(@Parameter(name = "paymentId", description = "Unique identifier of the target resource", required = true, in = ParameterIn.PATH) @PathVariable Long paymentId,
                                                                 @Valid @RequestBody ResolveDisputeRequest req) {
        return ResponseEntity.ok(adminService.resolvePaymentDispute(paymentId, req));
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error: check request body or query parameters", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden: your account role cannot perform this action", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Not found: the requested resource does not exist", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/users")
    @Operation(
        summary = "Search and list all users with filters",
        description = "List Users. Returns a paginated list of matching resources. Use the query parameters to filter and control sort order."
    )
    public ResponseEntity<Page<UserAdminDto>> listUsers(
            @Parameter(name = "role", description = "Query filter or pagination parameter", required = false, in = ParameterIn.QUERY) @RequestParam(required = false) String role,
            @Parameter(name = "status", description = "Query filter or pagination parameter", required = false, in = ParameterIn.QUERY) @RequestParam(required = false) String status,
            @Parameter(name = "search", description = "Query filter or pagination parameter", required = false, in = ParameterIn.QUERY) @RequestParam(required = false) String search,
            Pageable pageable) {
        return ResponseEntity.ok(adminService.listUsers(role, status, search, pageable));
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error: check request body or query parameters", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden: your account role cannot perform this action", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Not found: the requested resource does not exist", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @DeleteMapping("/users/{userId}")
    @Operation(
        summary = "Soft-delete a user account",
        description = "Delete User. Permanently removes the specified resource. This operation cannot be undone."
    )
    public ResponseEntity<ActionResponse> deleteUser(@Parameter(name = "id", description = "Unique identifier of the target resource", required = true, in = ParameterIn.PATH) @PathVariable Long userId) {
        return ResponseEntity.ok(adminService.deleteUser(userId));
    }
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error: check request body or query parameters", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden: your account role cannot perform this action", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Not found: the requested resource does not exist", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping("/tasks/{taskId}/assign")
    @Operation(
        summary = "Manually assign a task to a worker (admin override)",
        description = "Assign Task. Validates the incoming request body, persists the new resource, and returns the created entity with its server-generated ID."
    )
    public ResponseEntity<TaskDtos.TaskResponse> assignTask(
            @Parameter(name = "taskId", description = "Unique identifier of the target resource", required = true, in = ParameterIn.PATH) @PathVariable Long taskId,
            @Valid @RequestBody AssignTaskRequest req) {
        return ResponseEntity.ok(adminService.assignTask(taskId, req));
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error: check request body or query parameters", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden: your account role cannot perform this action", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Not found: the requested resource does not exist", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/tasks")
    @Operation(
        summary = "List all platform tasks with filters",
        description = "List Tasks. Returns a paginated list of matching resources. Use the query parameters to filter and control sort order."
    )
    public ResponseEntity<Page<TaskDtos.TaskResponse>> listTasks(
            @Parameter(name = "status", description = "Query filter or pagination parameter", required = false, in = ParameterIn.QUERY) @RequestParam(required = false) String status,
            @Parameter(name = "serviceType", description = "Query filter or pagination parameter", required = false, in = ParameterIn.QUERY) @RequestParam(required = false) String serviceType,
            Pageable pageable) {
        return ResponseEntity.ok(adminService.listTasks(status, serviceType, pageable));
    }
}
