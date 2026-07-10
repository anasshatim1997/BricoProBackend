package com.bricopro.auth.service;

import com.bricopro.config.security.LoginAttemptService;
import com.bricopro.notification.service.CommunicationService;
import com.bricopro.security.jwt.JwtService;
import com.bricopro.security.oauth2.RefreshTokenService;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.User.Status;
import com.bricopro.user.repository.UserRepository;
import com.bricopro.auth.dto.AuthDtos;
import com.bricopro.auth.entity.OtpCode;
import com.bricopro.auth.repository.OtpCodeRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Tag(name = "Auth Service", description = "Business logic for Auth Service")
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository       userRepository;
    private final OtpCodeRepository    otpCodeRepository;
    private final RefreshTokenService  refreshTokenService;
    private final JwtService           jwtService;
    private final PasswordEncoder      passwordEncoder;
    private final CommunicationService communicationService;
    private final LoginAttemptService  loginAttemptService;

    @Transactional
    public AuthDtos.MessageResponse register(AuthDtos.RegisterRequest req) {
        if (req.getEmail() != null && userRepository.existsByEmail(req.getEmail()))
            throw new IllegalArgumentException("Email already in use");
        if (req.getPhone() != null && userRepository.existsByPhone(req.getPhone()))
            throw new IllegalArgumentException("Phone already in use");

        User user = User.builder()
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .role(req.getRole())
                .status(Status.PENDING)
                .build();
        userRepository.save(user);

        if (req.getPhone() != null) {
            sendOtp(user, OtpCode.Purpose.PHONE_VERIFY);
        } else {
            user.setVerified(true);
            user.setStatus(Status.ACTIVE);
            userRepository.save(user);
            if (req.getEmail() != null) {
                communicationService.sendWelcomeEmail(req.getEmail(), req.getFirstName());
            }
        }
        return new AuthDtos.MessageResponse("Registration successful. Check your phone or email to verify.", user.getId());
    }

    @Transactional
    public AuthDtos.TokenResponse login(AuthDtos.LoginRequest req) {
        String lockKey = req.getEmail() != null ? req.getEmail() : req.getPhone();

        if (loginAttemptService.isBlocked(lockKey)) {
            long seconds = loginAttemptService.getLockRemainingSeconds(lockKey);
            throw new LockedException("Account temporarily locked. Retry in " + seconds + " seconds.");
        }

        User user;
        try {
            user = req.getEmail() != null
                    ? userRepository.findByEmail(req.getEmail())
                    .orElseThrow(() -> new BadCredentialsException("Invalid credentials"))
                    : userRepository.findByPhone(req.getPhone())
                    .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

            if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
                loginAttemptService.loginFailed(lockKey);
                int remaining = loginAttemptService.getRemainingAttempts(lockKey);
                throw new BadCredentialsException("Invalid credentials. " + remaining + " attempts remaining.");
            }
        } catch (BadCredentialsException e) {
            loginAttemptService.loginFailed(lockKey);
            throw e;
        }

        if (user.getStatus() == Status.SUSPENDED)
            throw new IllegalStateException("Account suspended. Contact support@bricopro.ma");
        if (!user.isVerified())
            throw new IllegalStateException("Account not verified. Please verify your phone or email.");

        loginAttemptService.loginSucceeded(lockKey);

        String access  = jwtService.generateAccessToken(user);
        String refresh = refreshTokenService.create(user).getToken();
        return new AuthDtos.TokenResponse(access, refresh, user.getId(), user.getRole().name());
    }

    @Transactional
    public AuthDtos.MessageResponse verifyOtp(AuthDtos.OtpVerifyRequest req) {
        OtpCode otp = otpCodeRepository
                .findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(req.getUserId(), OtpCode.Purpose.PHONE_VERIFY)
                .orElseThrow(() -> new IllegalArgumentException("No pending OTP found"));

        if (otp.getExpiresAt().isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("OTP expired. Request a new one.");
        if (!otp.getCode().equals(req.getCode()))
            throw new IllegalArgumentException("Invalid OTP");

        otp.setUsed(true);
        otpCodeRepository.save(otp);

        User user = otp.getUser();
        user.setVerified(true);
        user.setStatus(Status.ACTIVE);
        userRepository.save(user);

        if (user.getEmail() != null) {
            communicationService.sendAccountVerifiedEmail(user.getEmail(), user.getFirstName());
        }
        if (user.getPhone() != null) {
            communicationService.sendWhatsApp(
                    user.getPhone(),
                    "Bonjour " + user.getFirstName() + ", votre compte BricoPro est maintenant actif. Bienvenue !"
            );
        }

        return new AuthDtos.MessageResponse("Phone verified successfully");
    }

    @Transactional
    public AuthDtos.MessageResponse resendOtp(AuthDtos.ResendOtpRequest req) {
        User user = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        sendOtp(user, OtpCode.Purpose.PHONE_VERIFY);
        return new AuthDtos.MessageResponse("OTP resent successfully", user.getId());
    }

    @Transactional
    public AuthDtos.TokenResponse refresh(AuthDtos.RefreshRequest req) {
        return refreshTokenService.findValid(req.getRefreshToken())
                .map(rt -> {
                    String access     = jwtService.generateAccessToken(rt.getUser());
                    String newRefresh = refreshTokenService.create(rt.getUser()).getToken();
                    return new AuthDtos.TokenResponse(access, newRefresh,
                            rt.getUser().getId(), rt.getUser().getRole().name());
                })
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired refresh token"));
    }

    @Transactional
    public AuthDtos.MessageResponse logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
        return new AuthDtos.MessageResponse("Logged out successfully");
    }

    @Transactional
    public AuthDtos.MessageResponse forgotPassword(AuthDtos.ForgotPasswordRequest req) {
        User user = req.getEmail() != null
                ? userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"))
                : userRepository.findByPhone(req.getPhone())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        sendOtp(user, OtpCode.Purpose.PASSWORD_RESET);
        return new AuthDtos.MessageResponse("OTP sent for password reset");
    }

    @Transactional
    public AuthDtos.MessageResponse resetPassword(AuthDtos.ResetPasswordRequest req) {
        OtpCode otp = otpCodeRepository
                .findTopByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(req.getUserId(), OtpCode.Purpose.PASSWORD_RESET)
                .orElseThrow(() -> new IllegalArgumentException("No pending OTP"));

        if (otp.getExpiresAt().isBefore(LocalDateTime.now()))
            throw new IllegalArgumentException("OTP expired");
        if (!otp.getCode().equals(req.getCode()))
            throw new IllegalArgumentException("Invalid OTP");

        otp.setUsed(true);
        otpCodeRepository.save(otp);

        User user = otp.getUser();
        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);

        if (user.getEmail() != null) {
            communicationService.sendPasswordChangedEmail(user.getEmail(), user.getFirstName());
        }
        if (user.getPhone() != null) {
            communicationService.sendWhatsApp(
                    user.getPhone(),
                    "Bonjour " + user.getFirstName() + ", votre mot de passe BricoPro a été modifié avec succès."
            );
        }

        return new AuthDtos.MessageResponse("Password reset successfully");
    }

    private void sendOtp(User user, OtpCode.Purpose purpose) {
        String code = String.format("%06d", new Random().nextInt(999999));
        otpCodeRepository.save(OtpCode.builder()
                .user(user)
                .code(code)
                .purpose(purpose)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build());

        if (purpose == OtpCode.Purpose.PASSWORD_RESET) {
            if (user.getEmail() != null) {
                communicationService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), code);
            }
            if (user.getPhone() != null) {
                communicationService.sendWhatsApp(
                        user.getPhone(),
                        "BricoPro: Votre code de réinitialisation est " + code + ". Valable 10 minutes."
                );
            }
        } else {
            communicationService.sendOtp(user.getPhone(), user.getEmail(), user.getFirstName(), code);
        }
    }
}