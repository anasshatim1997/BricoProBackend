package com.bricopro.security.jwt;

import com.bricopro.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Token Blacklist Service", description = "Business logic for Token Blacklist Service")
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private static final String PREFIX = "bricopro:blacklist:";

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtProperties                 jwtProperties;
    private final JwtService                    jwtService;

    /**
     * Blacklist.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public void blacklist(String token) {
        try {
            
            long remainingMs = getRemainingMs(token);
            if (remainingMs > 0) {
                redisTemplate.opsForValue().set(
                        PREFIX + token,
                        "1",
                        Duration.ofMillis(remainingMs));
            }
        } catch (Exception e) {
            log.error("Failed to blacklist token in Redis: {}", e.getMessage());
        }
    }

    /**
     * Is Blacklisted.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public boolean isBlacklisted(String token) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + token));
        } catch (Exception e) {
            log.error("Redis blacklist check failed — allowing token: {}", e.getMessage());
            return false;   
        }
    }

    /**
     * Remove.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public void remove(String token) {
        try {
            redisTemplate.delete(PREFIX + token);
        } catch (Exception e) {
            log.error("Failed to remove token from blacklist: {}", e.getMessage());
        }
    }

    private long getRemainingMs(String token) {
        try {
            io.jsonwebtoken.Claims claims = jwtService.parseToken(token);
            long expiry = claims.getExpiration().getTime();
            return Math.max(0, expiry - System.currentTimeMillis());
        } catch (Exception e) {
            return jwtProperties.getAccessTokenExpirationMs();
        }
    }
}
