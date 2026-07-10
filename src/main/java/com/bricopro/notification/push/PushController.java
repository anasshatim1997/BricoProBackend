package com.bricopro.notification.push;

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

// ─────────────────────────────────────────────────────────────────────────────
// Controller (unchanged from before — just re-included for completeness)
// ─────────────────────────────────────────────────────────────────────────────
@RestController
@RequestMapping("/api/v1/push")
@RequiredArgsConstructor
@Tag(name = "Push Notifications")
@SecurityRequirement(name = "bearerAuth")
class PushController {

    private final PushNotificationService pushService;

    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Token registered"),
            @ApiResponse(responseCode = "401", description = "Unauthorised", content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping("/register")
    @Operation(summary = "Register FCM device token for push notifications")
    public ResponseEntity<Void> register(
            @AuthenticationPrincipal User user,
            @Parameter(name = "token", required = true, in = ParameterIn.QUERY) @RequestParam String token,
            @Parameter(name = "platform", required = true, in = ParameterIn.QUERY) @RequestParam DeviceToken.Platform platform) {
        pushService.registerToken(user.getId(), token, platform);
        return ResponseEntity.noContent().build();
    }

    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Token unregistered"),
            @ApiResponse(responseCode = "401", description = "Unauthorised", content = @Content(schema = @Schema(hidden = true)))
    })
    @DeleteMapping("/unregister")
    @Operation(summary = "Unregister FCM device token")
    public ResponseEntity<Void> unregister(
            @AuthenticationPrincipal User user,
            @Parameter(name = "token", required = true, in = ParameterIn.QUERY) @RequestParam String token) {
        pushService.unregisterToken(user.getId(), token);
        return ResponseEntity.noContent().build();
    }
}
