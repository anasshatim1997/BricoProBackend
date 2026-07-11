package com.bricopro.security;

import com.bricopro.auth.entity.RefreshToken;
import com.bricopro.auth.repository.RefreshTokenRepository;
import com.bricopro.config.JwtProperties;
import com.bricopro.security.jwt.JwtService;
import com.bricopro.security.jwt.TokenBlacklistService;
import com.bricopro.security.oauth2.RefreshTokenService;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.User.Role;
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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Token Services")
class TokenServicesTest {

    // ─── TOKEN BLACKLIST SERVICE ──────────────────────────────────────────────

    @Nested
    @DisplayName("TokenBlacklistService")
    class TokenBlacklistServiceTests {

        @Mock RedisTemplate<String, String> redisTemplate;
        @Mock JwtProperties jwtProperties;
        @Mock JwtService jwtService;
        @Mock ValueOperations<String, String> valueOps;

        @InjectMocks TokenBlacklistService tokenBlacklistService;

        @BeforeEach
        void setup() {
            lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        }

        @Test
        @DisplayName("isBlacklisted returns true when key exists in Redis")
        void isBlacklisted() {
            when(redisTemplate.hasKey("bricopro:blacklist:some.token.here")).thenReturn(true);

            assertThat(tokenBlacklistService.isBlacklisted("some.token.here")).isTrue();
        }

        @Test
        @DisplayName("isBlacklisted returns false when key does not exist")
        void isNotBlacklisted() {
            when(redisTemplate.hasKey(anyString())).thenReturn(false);

            assertThat(tokenBlacklistService.isBlacklisted("valid.token")).isFalse();
        }

        @Test
        @DisplayName("isBlacklisted returns false when Redis throws — fail open")
        void failOpenOnRedisError() {
            when(redisTemplate.hasKey(anyString())).thenThrow(new RuntimeException("Redis down"));

            assertThat(tokenBlacklistService.isBlacklisted("token")).isFalse();
        }

        @Test
        @DisplayName("blacklist() sets Redis key with TTL based on token remaining time")
        void blacklistSetsKey() {
            io.jsonwebtoken.Claims mockClaims = mock(io.jsonwebtoken.Claims.class);
            java.util.Date futureDate = new java.util.Date(System.currentTimeMillis() + 60_000L);
            when(mockClaims.getExpiration()).thenReturn(futureDate);
            when(jwtService.parseToken("some.token")).thenReturn(mockClaims);

            tokenBlacklistService.blacklist("some.token");

            verify(valueOps).set(eq("bricopro:blacklist:some.token"), eq("1"), any(java.time.Duration.class));
        }

        @Test
        @DisplayName("blacklist() does not set key for already-expired tokens")
        void doesNotBlacklistExpiredToken() {
            io.jsonwebtoken.Claims mockClaims = mock(io.jsonwebtoken.Claims.class);
            java.util.Date pastDate = new java.util.Date(System.currentTimeMillis() - 60_000L);
            when(mockClaims.getExpiration()).thenReturn(pastDate);
            when(jwtService.parseToken("expired.token")).thenReturn(mockClaims);

            tokenBlacklistService.blacklist("expired.token");

            verify(valueOps, never()).set(any(), any(), any(java.time.Duration.class));
        }

        @Test
        @DisplayName("remove() deletes Redis key")
        void removesKey() {
            tokenBlacklistService.remove("some.token");

            verify(redisTemplate).delete("bricopro:blacklist:some.token");
        }

        @Test
        @DisplayName("blacklist() silently handles Redis failure")
        void blacklistHandlesFailure() {
            when(jwtService.parseToken(anyString())).thenThrow(new RuntimeException("parse error"));
            when(jwtProperties.getAccessTokenExpirationMs()).thenReturn(900_000L);

            assertThatNoException().isThrownBy(() -> tokenBlacklistService.blacklist("bad.token"));
        }
    }

    // ─── REFRESH TOKEN SERVICE ────────────────────────────────────────────────

    @Nested
    @DisplayName("RefreshTokenService")
    class RefreshTokenServiceTests {

        @Mock RefreshTokenRepository refreshTokenRepository;
        @Mock JwtProperties jwtProperties;

        @InjectMocks RefreshTokenService refreshTokenService;

        private User user;

        @BeforeEach
        void setup() {
            user = User.builder().id(1L).email("test@bricopro.ma").role(Role.CLIENT).build();
            lenient().when(jwtProperties.getRefreshTokenExpirationMs()).thenReturn(604_800_000L);
        }

        @Test
        @DisplayName("create() revokes all existing tokens and saves a new one")
        void createsAndRevokesOldTokens() {
            RefreshToken saved = RefreshToken.builder()
                    .id(1L).user(user).token("new-uuid-token")
                    .expiresAt(LocalDateTime.now().plusDays(7)).revoked(false).build();
            when(refreshTokenRepository.save(any())).thenReturn(saved);

            RefreshToken token = refreshTokenService.create(user);

            verify(refreshTokenRepository).revokeAllByUserId(1L);
            assertThat(token.getToken()).isEqualTo("new-uuid-token");
            assertThat(token.isRevoked()).isFalse();
        }

        @Test
        @DisplayName("create() generates a non-blank UUID token")
        void generatesUniqueToken() {
            when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            RefreshToken token = refreshTokenService.create(user);

            assertThat(token.getToken()).isNotBlank();
            assertThat(token.getToken()).hasSize(36);
        }

        @Test
        @DisplayName("create() sets expiration based on JwtProperties")
        void setsExpiration() {
            when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            RefreshToken token = refreshTokenService.create(user);

            assertThat(token.getExpiresAt()).isAfter(LocalDateTime.now().plusDays(6));
            assertThat(token.getExpiresAt()).isBefore(LocalDateTime.now().plusDays(8));
        }

        @Test
        @DisplayName("findValid() returns token when it exists and is not revoked")
        void findValidToken() {
            RefreshToken rt = RefreshToken.builder()
                    .id(1L).user(user).token("valid-token")
                    .expiresAt(LocalDateTime.now().plusDays(5)).revoked(false).build();
            when(refreshTokenRepository.findByTokenAndRevokedFalse("valid-token"))
                    .thenReturn(Optional.of(rt));

            Optional<RefreshToken> found = refreshTokenService.findValid("valid-token");

            assertThat(found).isPresent();
        }

        @Test
        @DisplayName("findValid() returns empty when token is expired")
        void expiredTokenReturnsEmpty() {
            RefreshToken rt = RefreshToken.builder()
                    .id(1L).user(user).token("expired-token")
                    .expiresAt(LocalDateTime.now().minusDays(1))
                    .revoked(false).build();
            when(refreshTokenRepository.findByTokenAndRevokedFalse("expired-token"))
                    .thenReturn(Optional.of(rt));

            Optional<RefreshToken> found = refreshTokenService.findValid("expired-token");

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("findValid() returns empty when token not found")
        void tokenNotFound() {
            when(refreshTokenRepository.findByTokenAndRevokedFalse("unknown"))
                    .thenReturn(Optional.empty());

            Optional<RefreshToken> found = refreshTokenService.findValid("unknown");

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("revoke() marks token as revoked")
        void revokeToken() {
            RefreshToken rt = RefreshToken.builder()
                    .id(1L).user(user).token("to-revoke")
                    .expiresAt(LocalDateTime.now().plusDays(5)).revoked(false).build();
            when(refreshTokenRepository.findByTokenAndRevokedFalse("to-revoke"))
                    .thenReturn(Optional.of(rt));
            when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            refreshTokenService.revoke("to-revoke");

            assertThat(rt.isRevoked()).isTrue();
            verify(refreshTokenRepository).save(rt);
        }

        @Test
        @DisplayName("revoke() is a no-op when token not found")
        void revokeNonExistentToken() {
            when(refreshTokenRepository.findByTokenAndRevokedFalse(anyString()))
                    .thenReturn(Optional.empty());

            assertThatNoException().isThrownBy(() -> refreshTokenService.revoke("ghost-token"));
            verify(refreshTokenRepository, never()).save(any());
        }
    }
}