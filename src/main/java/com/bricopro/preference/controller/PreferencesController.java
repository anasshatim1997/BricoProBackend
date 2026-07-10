package com.bricopro.preference.controller;

import com.bricopro.preference.dto.UpdatePreferenceRequest;
import com.bricopro.preference.entity.UserPreference;
import com.bricopro.preference.service.UserPreferenceService;
import com.bricopro.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
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

@RestController
@RequestMapping("/api/v1/preferences")
@RequiredArgsConstructor
@Tag(name = "User Preferences")
@SecurityRequirement(name = "bearerAuth")
public class PreferencesController {

    private final UserPreferenceService preferenceService;

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping
    @Operation(summary = "Get my preferences (language, theme, notification settings)")
    public ResponseEntity<UserPreference> get(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(preferenceService.getOrCreate(user.getId()));
    }

    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Updated successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
    })
    @PatchMapping
    @Operation(summary = "Update preferences — only supplied fields are modified")
    public ResponseEntity<UserPreference> update(@AuthenticationPrincipal User user,
                                                  @RequestBody UpdatePreferenceRequest req) {
        return ResponseEntity.ok(preferenceService.update(user.getId(), req));
    }
}
