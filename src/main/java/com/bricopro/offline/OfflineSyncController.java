package com.bricopro.offline;

import com.bricopro.messaging.dto.MessagingDtos.SendMessageRequest;
import com.bricopro.messaging.service.MessagingService;
import com.bricopro.task.dto.TaskDtos.CreateTaskRequest;
import com.bricopro.task.dto.TaskDtos.UpdateTaskStatusRequest;
import com.bricopro.task.entity.Task.TaskStatus;
import com.bricopro.task.service.TaskService;
import com.bricopro.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/v1/sync")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Offline Sync")
@SecurityRequirement(name = "bearerAuth")
public class OfflineSyncController {

    @Schema(description = "Task Service", example = "value")
    private final TaskService      taskService;
    @Schema(description = "Messaging Service", example = "value")
    private final MessagingService messagingService;
    @Schema(description = "Object Mapper", example = "value")
    private final ObjectMapper     objectMapper;

    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Created successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error: check request body or query parameters", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden: your account role cannot perform this action", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Not found: the requested resource does not exist", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping("/actions")
    @Operation(
        summary = "Replay queued offline actions in order",
        description = "Sync Actions. Validates the incoming request body, persists the new resource, and returns the created entity with its server-generated ID."
    )
    public ResponseEntity<SyncResult> syncActions(@AuthenticationPrincipal User user,
                                                  @RequestBody SyncRequest request) {
        List<ActionResult> results = new ArrayList<>();

        for (QueuedAction action : request.getActions()) {
            String status = "SUCCESS";
            String error  = null;

            try {
                dispatch(action, user);
                log.info("Synced offline action: user={} type={} localId={}",
                        user.getId(), action.getType(), action.getLocalId());
            } catch (Exception e) {
                status = "FAILED";
                error  = e.getMessage();
                log.warn("Offline action failed: user={} type={} localId={} error={}",
                        user.getId(), action.getType(), action.getLocalId(), e.getMessage());
            }

            results.add(new ActionResult(action.getLocalId(), action.getType(),
                    status, error, System.currentTimeMillis()));
        }

        long succeeded = results.stream().filter(r -> "SUCCESS".equals(r.getStatus())).count();
        long failed    = results.stream().filter(r -> "FAILED".equals(r.getStatus())).count();

        return ResponseEntity.ok(new SyncResult(results, succeeded, failed, System.currentTimeMillis()));
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Validation error: check request body or query parameters", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Forbidden: your account role cannot perform this action", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Not found: the requested resource does not exist", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/status")
    @Operation(
        summary = "Ping — offline detection endpoint",
        description = "Method. Returns a paginated list of matching resources. Use the query parameters to filter and control sort order."
    )
    public ResponseEntity<Map<String, Object>> ping() {
        return ResponseEntity.ok(Map.of(
                "status",    "online",
                "timestamp", System.currentTimeMillis(),
                "server",    "BricoPro API"));
    }

private void dispatch(QueuedAction action, User actor) throws Exception {
        Map<String, Object> payload = action.getPayload();

        switch (action.getType()) {

            case "CREATE_TASK" -> {
                CreateTaskRequest req = objectMapper.convertValue(payload, CreateTaskRequest.class);
                taskService.create(actor, req);
            }

            case "UPDATE_TASK_STATUS" -> {
                Long taskId = getLong(payload, "taskId");
                UpdateTaskStatusRequest req = new UpdateTaskStatusRequest();
                req.setStatus(TaskStatus.valueOf(getString(payload, "status")));
                if (payload.containsKey("agreedPrice")) {
                    req.setAgreedPrice(new java.math.BigDecimal(getString(payload, "agreedPrice")));
                }
                if (payload.containsKey("cancellationReason")) {
                    req.setCancellationReason(getString(payload, "cancellationReason"));
                }
                taskService.updateStatus(actor, taskId, req);
            }

            case "ACCEPT_TASK" -> {
                Long taskId = getLong(payload, "taskId");
                taskService.acceptTask(actor, taskId);
            }

            case "SEND_MESSAGE" -> {
                Long conversationId = getLong(payload, "conversationId");
                SendMessageRequest req = new SendMessageRequest();
                req.setContent(getString(payload, "content"));
                if (payload.containsKey("mediaUrl"))
                    req.setMediaUrl(getString(payload, "mediaUrl"));
                messagingService.sendMessage(actor.getId(), conversationId, req);
            }

            case "MARK_MESSAGES_READ" -> {
                Long conversationId = getLong(payload, "conversationId");
                messagingService.markRead(conversationId, actor.getId());
            }

            default -> throw new IllegalArgumentException(
                    "Unknown action type: " + action.getType() +
                            ". Supported: CREATE_TASK, UPDATE_TASK_STATUS, ACCEPT_TASK, SEND_MESSAGE, MARK_MESSAGES_READ");
        }
    }

private Long getLong(Map<String, Object> payload, String key) {
        Object val = payload.get(key);
        if (val == null) throw new IllegalArgumentException("Missing field: " + key);
        return ((Number) val).longValue();
    }

    private String getString(Map<String, Object> payload, String key) {
        Object val = payload.get(key);
        if (val == null) throw new IllegalArgumentException("Missing field: " + key);
        return val.toString();
    }

@Data
    @Schema(description = "Queued Action")
    public static class QueuedAction {
        @Schema(description = "Local Id", example = "example")
        private String localId;
        @Schema(description = "Type", example = "example")
        private String type;
        private java.util.Map<String, Object> payload;
        @Schema(description = "Timestamp", example = "value")
        private long timestamp;
    }

    @Data
    @Schema(description = "Request payload for: Sync.")
    public static class SyncRequest {
        @Schema(description = "Actions", example = "[]")
        private List<QueuedAction> actions;
    }

    @Data
    @RequiredArgsConstructor
    @Schema(description = "Action Result")
    public static class ActionResult {
        @Schema(description = "Local Id", example = "1")
        private final String localId;
        @Schema(description = "Type", example = "value")
        private final String type;
        @Schema(description = "Current status of the entity", example = "value")
        private final String status;
        @Schema(description = "Error", example = "value")
        private final String error;
        @Schema(description = "Synced At", example = "value")
        private final long syncedAt;
    }

    @Data
    @RequiredArgsConstructor
    @Schema(description = "Sync Result")
    public static class SyncResult {
        @Schema(description = "Results", example = "value")
        private final List<ActionResult> results;
        @Schema(description = "Succeeded", example = "value")
        private final long succeeded;
        @Schema(description = "Failed", example = "value")
        private final long failed;
        @Schema(description = "Synced At", example = "value")
        private final long syncedAt;
    }
}
