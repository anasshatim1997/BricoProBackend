package com.bricopro.subscription;

import com.bricopro.task.entity.Task;
import com.bricopro.task.entity.Task.TaskStatus;
import com.bricopro.task.repository.TaskRepository;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.WorkerProfile.ServiceType;
import com.bricopro.user.repository.UserRepository;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@Entity
@Table(name = "recurring_tasks")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RecurringTask {

    public enum Frequency { DAILY, WEEKLY, BIWEEKLY, MONTHLY }
    public enum RecurringStatus { ACTIVE, PAUSED, CANCELLED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    User client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "preferred_worker_id")
    User preferredWorker;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    ServiceType serviceType;

    @Column(nullable = false)
    String title;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(columnDefinition = "TEXT")
    String address;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    Frequency frequency;

    LocalTime preferredTime;
    LocalDate nextScheduledDate;
    LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    RecurringStatus status = RecurringStatus.ACTIVE;

    @CreationTimestamp
    LocalDateTime createdAt;
}

@RestController
@RequestMapping("/api/v1/recurring-tasks")
@RequiredArgsConstructor
@Tag(name = "Recurring Tasks")
@SecurityRequirement(name = "bearerAuth")
class RecurringTaskController {

    @Schema(description = "Service", example = "value")
    private final RecurringTaskService service;

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
        summary = "Create a recurring service subscription (weekly cleaning, monthly maintenance)",
        description = "Create. Validates the incoming request body, persists the new resource, and returns the created entity with its server-generated ID."
    )
    /**
     * Create.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public ResponseEntity<RecurringTask> create(@AuthenticationPrincipal User user,
                                                 @RequestBody CreateRecurringRequest req) {
        return ResponseEntity.ok(service.create(user.getId(), req));
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error: check request body or query parameters", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden: your account role cannot perform this action", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Not found: the requested resource does not exist", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(
        summary = "List my recurring subscriptions",
        description = "List. Returns a paginated list of matching resources. Use the query parameters to filter and control sort order."
    )
    /**
     * List.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public ResponseEntity<Page<RecurringTask>> list(@AuthenticationPrincipal User user, Pageable pageable) {
        return ResponseEntity.ok(service.getForClient(user.getId(), pageable));
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Updated successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error: check request body or query parameters", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden: your account role cannot perform this action", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Not found: the requested resource does not exist", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @PatchMapping("/{id}/pause")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(
        summary = "Pause a recurring task",
        description = "Pause. Updates the resource identified by the path parameter. Only fields supplied in the request body are modified; others remain unchanged."
    )
    /**
     * Pause.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public ResponseEntity<RecurringTask> pause(@AuthenticationPrincipal User user, @Parameter(name = "id", description = "Unique identifier of the target resource", required = true, in = ParameterIn.PATH) @PathVariable Long id) {
        return ResponseEntity.ok(service.pause(id, user.getId()));
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Deleted successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error: check request body or query parameters", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden: your account role cannot perform this action", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Not found: the requested resource does not exist", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(
        summary = "Cancel a recurring subscription",
        description = "Cancel. Permanently removes the specified resource. This operation cannot be undone."
    )
    /**
     * Cancel.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public ResponseEntity<RecurringTask> cancel(@AuthenticationPrincipal User user, @Parameter(name = "id", description = "Unique identifier of the target resource", required = true, in = ParameterIn.PATH) @PathVariable Long id) {
        return ResponseEntity.ok(service.cancel(id, user.getId()));
    }

}
