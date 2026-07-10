package com.bricopro.auth.dto;

import com.bricopro.user.entity.User.Role;
import jakarta.validation.constraints.*;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;

public class AuthDtos {

    @Data
    @Schema(description = "Request payload for: Register.")
    public static class RegisterRequest {
        @NotBlank @Size(min = 2, max = 100)
        @Schema(description = "First name of the user", example = "example")
        private String firstName;
        @NotBlank @Size(min = 2, max = 100)
        @Schema(description = "Last name of the user", example = "example")
        private String lastName;
        @Email
        @Schema(description = "Email address of the user", example = "example")
        private String email;
        @Pattern(regexp = "^\\+?[0-9]{9,15}$")
        @Schema(description = "Phone number in international format (e.g. +212XXXXXXXXX)", example = "example")
        private String phone;
        @Size(min = 8)
        @Schema(description = "Password — minimum 8 characters", example = "example")
        private String password;
        @NotNull
        @Schema(description = "User role: CLIENT, WORKER, or ADMIN", example = "value")
        private Role role;
    }

    @Data
    @Schema(description = "Request payload for: Login.")
    public static class LoginRequest {
        @Schema(description = "Email address of the user", example = "example")
        private String email;
        @Schema(description = "Phone number in international format (e.g. +212XXXXXXXXX)", example = "example")
        private String phone;
        @NotBlank
        @Schema(description = "Password — minimum 8 characters", example = "example")
        private String password;
    }

    @Data
    @Schema(description = "Request payload for: Otp Verify.")
    public static class OtpVerifyRequest {
        @NotNull
        @Schema(description = "ID of the target user", example = "1")
        private Long userId;
        @NotBlank @Size(min = 4, max = 10)
        @Schema(description = "One-time verification code (OTP)", example = "example")
        private String code;
    }

    @Data
    @Schema(description = "Request payload for: Resend OTP.")
    public static class ResendOtpRequest {
        @NotNull
        @Schema(description = "ID of the target user", example = "1")
        private Long userId;
    }

    @Data
    @Schema(description = "Request payload for: Refresh.")
    public static class RefreshRequest {
        @NotBlank
        @Schema(description = "Long-lived token used to renew the access token", example = "example")
        private String refreshToken;
    }

    @Data
    @Schema(description = "Request payload for: Forgot Password.")
    public static class ForgotPasswordRequest {
        @Schema(description = "Email address of the user", example = "example")
        private String email;
        @Schema(description = "Phone number in international format (e.g. +212XXXXXXXXX)", example = "example")
        private String phone;
    }

    @Data
    @Schema(description = "Request payload for Google OAuth2 login")
    public static class GoogleOAuthRequest {
        @NotBlank
        private String idToken;
    }

    @Data
    @Schema(description = "Request payload for Facebook OAuth2 login")
    public static class FacebookOAuthRequest {
        @NotBlank
        private String accessToken;
    }

    @Data
    @Schema(description = "Request payload for: Reset Password.")
    public static class ResetPasswordRequest {
        @NotNull
        @Schema(description = "ID of the target user", example = "1")
        private Long userId;
        @NotBlank
        @Schema(description = "One-time verification code (OTP)", example = "example")
        private String code;
        @NotBlank @Size(min = 8)
        @Schema(description = "New password — minimum 8 characters", example = "example")
        private String newPassword;
    }

    @Data
    @Schema(description = "Response body returned by: Token.")
    public static class TokenResponse {
        @Schema(description = "Short-lived JWT access token (15 min default)", example = "example")
        private String accessToken;
        @Schema(description = "Long-lived token used to renew the access token", example = "example")
        private String refreshToken;
        private String tokenType = "Bearer";
        @Schema(description = "ID of the target user", example = "1")
        private Long userId;
        @Schema(description = "User role: CLIENT, WORKER, or ADMIN", example = "example")
        private String role;

        public TokenResponse(String accessToken, String refreshToken, Long userId, String role) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.userId = userId;
            this.role = role;
        }
    }

    @Data
    @Schema(description = "Response body returned by: Message.")
    public static class MessageResponse {
        @Schema(description = "Human-readable response message", example = "example")
        private String message;
        @Schema(description = "ID of the registered user", example = "1")
        private Long userId;
        public MessageResponse(String message) { this.message = message; }
        public MessageResponse(String message, Long userId) {
            this.message = message;
            this.userId  = userId;
        }
    }
}