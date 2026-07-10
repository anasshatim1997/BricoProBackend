package com.bricopro.auth;

import com.bricopro.auth.dto.AuthDtos.*;
import com.bricopro.auth.entity.OtpCode;
import com.bricopro.auth.entity.RefreshToken;
import com.bricopro.auth.repository.OtpCodeRepository;
import com.bricopro.auth.service.AuthService;
import com.bricopro.config.security.LoginAttemptService;
import com.bricopro.notification.service.CommunicationService;
import com.bricopro.security.jwt.JwtService;
import com.bricopro.security.oauth2.RefreshTokenService;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.User.Role;
import com.bricopro.user.entity.User.Status;
import com.bricopro.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock OtpCodeRepository otpCodeRepository;
    @Mock RefreshTokenService refreshTokenService;
    @Mock JwtService jwtService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock CommunicationService communicationService;
    @Mock LoginAttemptService loginAttemptService;

    @InjectMocks AuthService authService;

    private User activeUser;

    @BeforeEach
    void setup() {
        activeUser = User.builder()
                .id(1L)
                .email("test@bricopro.ma")
                .phone("+212600000001")
                .passwordHash("hashed_password")
                .firstName("Hassan")
                .lastName("Alami")
                .role(Role.CLIENT)
                .status(Status.ACTIVE)
                .isVerified(true)
                .build();
    }

    // ─── REGISTER ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("registers with phone → sends OTP, status PENDING")
        void registerWithPhone() {
            RegisterRequest req = new RegisterRequest();
            req.setFirstName("Youssef");
            req.setLastName("Benali");
            req.setPhone("+212611111111");
            req.setPassword("password123");
            req.setRole(Role.CLIENT);

            when(userRepository.existsByPhone(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(otpCodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MessageResponse res = authService.register(req);

            assertThat(res.getMessage()).contains("Registration");
            verify(communicationService).sendOtp(eq("+212611111111"), isNull(), eq("Youssef"), anyString());
        }

        @Test
        @DisplayName("registers email-only → activates immediately, sends welcome email")
        void registerEmailOnly() {
            RegisterRequest req = new RegisterRequest();
            req.setFirstName("Sara");
            req.setLastName("El Amrani");
            req.setEmail("sara@example.com");
            req.setPassword("password123");
            req.setRole(Role.WORKER);

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            authService.register(req);

            verify(communicationService).sendWelcomeEmail(eq("sara@example.com"), eq("Sara"));
        }

        @Test
        @DisplayName("throws when email already in use")
        void duplicateEmail() {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("used@bricopro.ma");
            req.setPassword("pass12345");
            req.setRole(Role.CLIENT);

            when(userRepository.existsByEmail("used@bricopro.ma")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Email");
        }

        @Test
        @DisplayName("throws when phone already in use")
        void duplicatePhone() {
            RegisterRequest req = new RegisterRequest();
            req.setPhone("+212600000001");
            req.setPassword("pass12345");
            req.setRole(Role.CLIENT);

            when(userRepository.existsByPhone("+212600000001")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Phone");
        }
    }

    // ─── LOGIN ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("successful login returns access + refresh tokens")
        void successfulLogin() {
            LoginRequest req = new LoginRequest();
            req.setEmail("test@bricopro.ma");
            req.setPassword("correct_pass");

            when(loginAttemptService.isBlocked(anyString())).thenReturn(false);
            when(userRepository.findByEmail("test@bricopro.ma")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("correct_pass", "hashed_password")).thenReturn(true);
            when(jwtService.generateAccessToken(activeUser)).thenReturn("access_token");

            RefreshToken rt = new RefreshToken();
            rt.setToken("refresh_token");
            when(refreshTokenService.create(activeUser)).thenReturn(rt);

            TokenResponse res = authService.login(req);

            assertThat(res.getAccessToken()).isEqualTo("access_token");
            assertThat(res.getRefreshToken()).isEqualTo("refresh_token");
            assertThat(res.getRole()).isEqualTo("CLIENT");
        }

        @Test
        @DisplayName("throws LockedException when account is blocked")
        void blockedAccount() {
            LoginRequest req = new LoginRequest();
            req.setEmail("blocked@bricopro.ma");
            req.setPassword("any");

            when(loginAttemptService.isBlocked("blocked@bricopro.ma")).thenReturn(true);
            when(loginAttemptService.getLockRemainingSeconds(anyString())).thenReturn(300L);

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(LockedException.class);
        }

        @Test
        @DisplayName("throws BadCredentialsException for wrong password")
        void wrongPassword() {
            LoginRequest req = new LoginRequest();
            req.setEmail("test@bricopro.ma");
            req.setPassword("wrong_pass");

            when(loginAttemptService.isBlocked(anyString())).thenReturn(false);
            when(userRepository.findByEmail("test@bricopro.ma")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("wrong_pass", "hashed_password")).thenReturn(false);
            when(loginAttemptService.getRemainingAttempts(anyString())).thenReturn(4);

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(BadCredentialsException.class);
            verify(loginAttemptService, times(2)).loginFailed(anyString());
        }

        @Test
        @DisplayName("throws IllegalStateException for suspended user")
        void suspendedUser() {
            activeUser.setStatus(Status.SUSPENDED);
            LoginRequest req = new LoginRequest();
            req.setEmail("test@bricopro.ma");
            req.setPassword("correct_pass");

            when(loginAttemptService.isBlocked(anyString())).thenReturn(false);
            when(userRepository.findByEmail("test@bricopro.ma")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("suspended");
        }

        @Test
        @DisplayName("throws IllegalStateException for unverified user")
        void unverifiedUser() {
            activeUser.setVerified(false);
            LoginRequest req = new LoginRequest();
            req.setEmail("test@bricopro.ma");
            req.setPassword("correct_pass");

            when(loginAttemptService.isBlocked(anyString())).thenReturn(false);
            when(userRepository.findByEmail("test@bricopro.ma")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

            assertThatThrownBy(() -> authService.login(req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("verified");
        }

        @Test
        @DisplayName("login by phone works correctly")
        void loginByPhone() {
            LoginRequest req = new LoginRequest();
            req.setPhone("+212600000001");
            req.setPassword("correct_pass");

            when(loginAttemptService.isBlocked(anyString())).thenReturn(false);
            when(userRepository.findByPhone("+212600000001")).thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(jwtService.generateAccessToken(any())).thenReturn("tok");
            RefreshToken rt = new RefreshToken(); rt.setToken("ref");
            when(refreshTokenService.create(any())).thenReturn(rt);

            TokenResponse res = authService.login(req);
            assertThat(res.getAccessToken()).isEqualTo("tok");
        }
    }

    // ─── VERIFY OTP ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("verifyOtp()")
    class VerifyOtp {

        @Test
        @DisplayName("valid OTP activates account")
        void validOtp() {
            OtpVerifyRequest req = new OtpVerifyRequest();
            req.setUserId(1L);
            req.setCode("123456");

            OtpCode otp = OtpCode.builder()
                    .user(activeUser)
                    .code("123456")
                    .purpose(OtpCode.Purpose.PHONE_VERIFY)
                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                    .used(false)
                    .build();

            when(otpCodeRepository.findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                    1L, OtpCode.Purpose.PHONE_VERIFY)).thenReturn(Optional.of(otp));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(otpCodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MessageResponse res = authService.verifyOtp(req);
            assertThat(res.getMessage()).containsIgnoringCase("verified");
            assertThat(activeUser.isVerified()).isTrue();
            assertThat(activeUser.getStatus()).isEqualTo(Status.ACTIVE);
        }

        @Test
        @DisplayName("expired OTP throws IllegalArgumentException")
        void expiredOtp() {
            OtpVerifyRequest req = new OtpVerifyRequest();
            req.setUserId(1L);
            req.setCode("123456");

            OtpCode otp = OtpCode.builder()
                    .code("123456")
                    .purpose(OtpCode.Purpose.PHONE_VERIFY)
                    .expiresAt(LocalDateTime.now().minusMinutes(1))
                    .used(false)
                    .build();

            when(otpCodeRepository.findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                    1L, OtpCode.Purpose.PHONE_VERIFY)).thenReturn(Optional.of(otp));

            assertThatThrownBy(() -> authService.verifyOtp(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        @DisplayName("wrong OTP code throws")
        void wrongOtpCode() {
            OtpVerifyRequest req = new OtpVerifyRequest();
            req.setUserId(1L);
            req.setCode("000000");

            OtpCode otp = OtpCode.builder()
                    .code("123456")
                    .purpose(OtpCode.Purpose.PHONE_VERIFY)
                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                    .used(false)
                    .build();

            when(otpCodeRepository.findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                    1L, OtpCode.Purpose.PHONE_VERIFY)).thenReturn(Optional.of(otp));

            assertThatThrownBy(() -> authService.verifyOtp(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid");
        }
    }

    // ─── LOGOUT ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("logout() revokes refresh token")
    void logout() {
        MessageResponse res = authService.logout("some_refresh_token");
        verify(refreshTokenService).revoke("some_refresh_token");
        assertThat(res.getMessage()).containsIgnoringCase("logged out");
    }

    // ─── FORGOT / RESET PASSWORD ──────────────────────────────────────────────

    @Nested
    @DisplayName("forgotPassword()")
    class ForgotPassword {

        @Test
        @DisplayName("sends OTP by email")
        void byEmail() {
            ForgotPasswordRequest req = new ForgotPasswordRequest();
            req.setEmail("test@bricopro.ma");

            when(userRepository.findByEmail("test@bricopro.ma")).thenReturn(Optional.of(activeUser));
            when(otpCodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            authService.forgotPassword(req);
            verify(communicationService).sendPasswordResetEmail(eq("test@bricopro.ma"), eq("Hassan"), anyString());
        }

        @Test
        @DisplayName("throws when user not found")
        void userNotFound() {
            ForgotPasswordRequest req = new ForgotPasswordRequest();
            req.setEmail("nobody@bricopro.ma");

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.forgotPassword(req))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("resetPassword()")
    class ResetPassword {

        @Test
        @DisplayName("valid OTP resets password and notifies user")
        void validReset() {
            ResetPasswordRequest req = new ResetPasswordRequest();
            req.setUserId(1L);
            req.setCode("654321");
            req.setNewPassword("newPassword!1");

            OtpCode otp = OtpCode.builder()
                    .user(activeUser)
                    .code("654321")
                    .purpose(OtpCode.Purpose.PASSWORD_RESET)
                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                    .used(false)
                    .build();

            when(otpCodeRepository.findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                    1L, OtpCode.Purpose.PASSWORD_RESET)).thenReturn(Optional.of(otp));
            when(passwordEncoder.encode("newPassword!1")).thenReturn("new_hash");
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(otpCodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            authService.resetPassword(req);

            assertThat(activeUser.getPasswordHash()).isEqualTo("new_hash");
            verify(communicationService).sendPasswordChangedEmail(eq("test@bricopro.ma"), eq("Hassan"));
        }
    }
}
