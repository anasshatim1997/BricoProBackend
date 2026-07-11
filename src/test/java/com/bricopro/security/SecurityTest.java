package com.bricopro.security;

import com.bricopro.config.JwtProperties;
import com.bricopro.config.security.LoginAttemptService;
import com.bricopro.security.jwt.JwtService;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.User.Role;
import com.bricopro.user.entity.User.Status;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Security Layer")
class SecurityTest {

    // ─── JWT SERVICE ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("JwtService")
    class JwtServiceTests {

        private JwtService jwtService;
        private User testUser;

        @BeforeEach
        void setup() {
            JwtProperties props = new JwtProperties();
            props.setSecret("dGVzdC1zZWNyZXQta2V5LWZvci1icmljb3Byby10ZXN0aW5nLW9ubHktbm90LXNob3J0");
            props.setAccessTokenExpirationMs(3_600_000L);
            props.setRefreshTokenExpirationMs(604_800_000L);

            jwtService = new JwtService(props);

            testUser = User.builder()
                    .id(42L)
                    .email("jwt@bricopro.ma")
                    .role(Role.WORKER)
                    .status(Status.ACTIVE)
                    .build();
        }

        @Test
        @DisplayName("generates a valid, parseable JWT token")
        void generatesValidToken() {
            String token = jwtService.generateAccessToken(testUser);
            assertThat(token).isNotBlank();
            assertThat(jwtService.isValid(token)).isTrue();
        }

        @Test
        @DisplayName("extracts correct userId from token")
        void extractsUserId() {
            String token = jwtService.generateAccessToken(testUser);
            assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
        }

        @Test
        @DisplayName("extracts correct role from token")
        void extractsRole() {
            String token = jwtService.generateAccessToken(testUser);
            assertThat(jwtService.extractRole(token)).isEqualTo("WORKER");
        }

        @Test
        @DisplayName("returns false for an invalid/tampered token")
        void invalidToken() {
            assertThat(jwtService.isValid("not.a.real.token")).isFalse();
        }

        @Test
        @DisplayName("returns false for expired token")
        void expiredToken() {
            JwtProperties shortProps = new JwtProperties();
            shortProps.setSecret("dGVzdC1zZWNyZXQta2V5LWZvci1icmljb3Byby10ZXN0aW5nLW9ubHktbm90LXNob3J0");
            shortProps.setAccessTokenExpirationMs(-1L);
            JwtService shortJwt = new JwtService(shortProps);

            String token = shortJwt.generateAccessToken(testUser);
            assertThat(shortJwt.isValid(token)).isFalse();
        }

        @Test
        @DisplayName("different users generate different tokens")
        void uniqueTokensPerUser() {
            User other = User.builder().id(99L).email("other@test.ma").role(Role.CLIENT).build();
            String t1 = jwtService.generateAccessToken(testUser);
            String t2 = jwtService.generateAccessToken(other);
            assertThat(t1).isNotEqualTo(t2);
        }
    }

    // ─── LOGIN ATTEMPT SERVICE ────────────────────────────────────────────────

    @Nested
    @DisplayName("LoginAttemptService")
    class LoginAttemptServiceTests {

        @Mock RedisTemplate<String, String> redisTemplate;
        @Mock ValueOperations<String, String> valueOps;

        @InjectMocks LoginAttemptService loginAttemptService;

        @BeforeEach
        void setup() {
            lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        }

        @Test
        @DisplayName("isBlocked returns true when Redis key exists")
        void isBlocked() {
            when(redisTemplate.hasKey("bricopro:login:locked:user@test.ma")).thenReturn(true);
            assertThat(loginAttemptService.isBlocked("user@test.ma")).isTrue();
        }

        @Test
        @DisplayName("isBlocked returns false when Redis key does not exist")
        void isNotBlocked() {
            when(redisTemplate.hasKey(anyString())).thenReturn(false);
            assertThat(loginAttemptService.isBlocked("user@test.ma")).isFalse();
        }

        @Test
        @DisplayName("loginSucceeded removes attempt and locked keys")
        void loginSucceededClearsKeys() {
            loginAttemptService.loginSucceeded("user@test.ma");
            verify(redisTemplate).delete("bricopro:login:attempts:user@test.ma");
            verify(redisTemplate).delete("bricopro:login:locked:user@test.ma");
        }

        @Test
        @DisplayName("loginFailed increments counter in Redis")
        void loginFailedIncrements() {
            when(valueOps.increment("bricopro:login:attempts:user@test.ma")).thenReturn(1L);
            loginAttemptService.loginFailed("user@test.ma");
            verify(valueOps).increment("bricopro:login:attempts:user@test.ma");
        }

        @Test
        @DisplayName("loginFailed sets TTL on first attempt")
        void loginFailedSetsTTL() {
            when(valueOps.increment(anyString())).thenReturn(1L);
            loginAttemptService.loginFailed("user@test.ma");
            verify(redisTemplate).expire(
                    eq("bricopro:login:attempts:user@test.ma"),
                    eq(Duration.ofMinutes(30)));
        }

        @Test
        @DisplayName("loginFailed sets locked key after 5 attempts")
        void locksAfterFiveAttempts() {
            when(valueOps.increment(anyString())).thenReturn(5L);
            loginAttemptService.loginFailed("user@test.ma");
            verify(valueOps).set(eq("bricopro:login:locked:user@test.ma"), eq("1"), any(Duration.class));
        }

        @Test
        @DisplayName("getRemainingAttempts returns correct count")
        void remainingAttempts() {
            when(valueOps.get("bricopro:login:attempts:user@test.ma")).thenReturn("3");
            assertThat(loginAttemptService.getRemainingAttempts("user@test.ma")).isEqualTo(2);
        }

        @Test
        @DisplayName("isBlocked returns false when Redis throws exception — fail open")
        void failOpen() {
            when(redisTemplate.hasKey(anyString())).thenThrow(new RuntimeException("Redis down"));
            assertThat(loginAttemptService.isBlocked("user@test.ma")).isFalse();
        }
    }
}