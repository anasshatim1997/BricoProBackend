package com.bricopro.notification.controller;

import com.bricopro.notification.entity.Notification;
import com.bricopro.notification.service.NotificationService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app notifications — list, count unread, and mark as read")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(
            summary = "List notifications",
            description = "Returns a paginated, newest-first list of notifications for the authenticated user. " +
                    "Supports standard Spring Pageable query params: page, size, sort."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notification page returned successfully"),
            @ApiResponse(responseCode = "401", description = "JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping
    public ResponseEntity<Page<Notification>> list(@AuthenticationPrincipal User user, Pageable pageable) {
        return ResponseEntity.ok(notificationService.getForUser(user.getId(), pageable));
    }

    @Operation(
            summary = "Get unread count",
            description = "Returns the total number of unread notifications for the authenticated user. " +
                    "Use this to drive the badge counter in your app's navigation bar."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Unread count returned — { \"count\": N }"),
            @ApiResponse(responseCode = "401", description = "JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(user.getId())));
    }

    @Operation(
            summary = "Mark all notifications as read",
            description = "Marks every unread notification for the authenticated user as read in one call. " +
                    "Useful when the user opens the notifications screen."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "All notifications marked as read"),
            @ApiResponse(responseCode = "401", description = "JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(hidden = true)))
    })
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal User user) {
        notificationService.markAllRead(user.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Mark one notification as read",
            description = "Marks a single notification as read by its ID. " +
                    "Call this when the user taps a specific notification item."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Notification marked as read"),
            @ApiResponse(responseCode = "401", description = "JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Notification not found", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(hidden = true)))
    })
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(
            @Parameter(name = "id", description = "ID of the notification to mark as read", required = true, in = ParameterIn.PATH)
            @PathVariable Long id) {
        notificationService.markRead(id);
        return ResponseEntity.noContent().build();
    }
}