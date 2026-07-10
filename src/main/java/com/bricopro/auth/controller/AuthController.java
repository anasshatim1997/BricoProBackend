package com.bricopro.auth.controller;

import com.bricopro.auth.dto.AuthDtos.*;
import com.bricopro.auth.service.AuthService;
import com.bricopro.security.oauth2.OAuth2ExchangeCodeService;
import com.bricopro.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, OTP verification, token refresh and logout")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final OAuth2ExchangeCodeService exchangeCodeService;

    @Operation(
            summary = "Register a new user",
            description = "Creates a CLIENT or WORKER account. If a phone number is provided, an OTP is sent for verification. " +
                    "If only an email is provided, the account is activated immediately and a welcome email is sent."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Account created — check your phone or email to verify"),
            @ApiResponse(responseCode = "400", description = "Validation failed — email or phone already in use, or required fields missing", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));
    }

    @Operation(
            summary = "Login with email or phone",
            description = "Authenticates the user with email/phone and password. Returns a short-lived JWT access token " +
                    "and a long-lived refresh token. Account is temporarily locked after 5 failed attempts."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful — returns access and refresh tokens"),
            @ApiResponse(responseCode = "400", description = "Invalid credentials or missing fields", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Account suspended or not yet verified", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "423", description = "Account temporarily locked due to too many failed attempts", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @Operation(
            summary = "Verify phone OTP",
            description = "Validates the 6-digit OTP sent to the user's phone after registration. " +
                    "On success, the account is activated and a confirmation email is sent."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Phone verified — account is now active"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired OTP", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "No pending OTP found for this user", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping("/verify-otp")
    public ResponseEntity<MessageResponse> verifyOtp(@Valid @RequestBody OtpVerifyRequest req) {
        return ResponseEntity.ok(authService.verifyOtp(req));
    }

    @Operation(
            summary = "Resend phone verification OTP",
            description = "Generates and sends a new 6-digit OTP to the user's phone. " +
                    "Any previously active OTP for this user is implicitly superseded by the new one."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "New OTP sent to user's phone"),
            @ApiResponse(responseCode = "404", description = "No user found with the provided ID", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping("/resend-otp")
    public ResponseEntity<MessageResponse> resendOtp(@Valid @RequestBody ResendOtpRequest req) {
        return ResponseEntity.ok(authService.resendOtp(req));
    }

    @Operation(
            summary = "Rotate access token",
            description = "Exchanges a valid refresh token for a new access token and a new refresh token. " +
                    "The old refresh token is invalidated immediately after use."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "New token pair issued successfully"),
            @ApiResponse(responseCode = "400", description = "Refresh token is invalid, expired or already revoked", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        return ResponseEntity.ok(authService.refresh(req));
    }

    @Operation(
            summary = "Logout",
            description = "Revokes the provided refresh token server-side. " +
                    "The client must also discard the access token locally. " +
                    "Any subsequent request using this refresh token will be rejected."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logged out successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT access token", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(hidden = true)))
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(@Valid @RequestBody RefreshRequest req) {
        return ResponseEntity.ok(authService.logout(req.getRefreshToken()));
    }

    @Operation(
            summary = "Request password reset OTP",
            description = "Sends a 6-digit OTP to the user's email or WhatsApp to initiate a password reset. " +
                    "The OTP is valid for 10 minutes. Call /reset-password next with the received code."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OTP sent to email or WhatsApp"),
            @ApiResponse(responseCode = "400", description = "Missing email or phone field", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "No account found with this email or phone", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        return ResponseEntity.ok(authService.forgotPassword(req));
    }

    @Operation(
            summary = "Reset password using OTP",
            description = "Validates the OTP received from /forgot-password and updates the user's password. " +
                    "A confirmation email is sent after a successful reset. " +
                    "All active sessions remain valid — the user must log out manually if needed."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired OTP, or new password does not meet requirements", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "No pending password reset OTP found for this user", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        return ResponseEntity.ok(authService.resetPassword(req));
    }

    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Boolean>> checkEmail(@RequestParam String email) {
        boolean exists = userRepository.existsByEmail(email);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @Operation(
            summary = "Exchange an OAuth2 callback code for real tokens",
            description = "After a successful Google/Facebook login, the mobile app is redirected to its deep link " +
                    "with a short-lived, single-use `code` query parameter instead of the tokens themselves. " +
                    "Call this endpoint immediately with that code to receive the actual access and refresh tokens. " +
                    "The code expires after 30 seconds and can only be exchanged once."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tokens issued successfully"),
            @ApiResponse(responseCode = "400", description = "Code is invalid, expired, or already used", content = @Content(schema = @Schema(hidden = true)))
    })
    @PostMapping("/oauth2/exchange")
    public ResponseEntity<TokenResponse> exchangeOAuth2Code(@RequestParam String code) {
        OAuth2ExchangeCodeService.ExchangePayload payload = exchangeCodeService.consume(code)
                .orElseThrow(() -> new IllegalArgumentException("Invalid, expired, or already-used code"));
        return ResponseEntity.ok(new TokenResponse(
                payload.accessToken(), payload.refreshToken(), payload.userId(), payload.role()));
    }
}