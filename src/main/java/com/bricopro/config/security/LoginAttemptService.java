package com.bricopro.config.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Login Attempt Service", description = "Business logic for Login Attempt Service")
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginAttemptService {

    private static final int    MAX_ATTEMPTS  = 5;
    private static final int    LOCK_MINUTES  = 15;

    private static final String ATTEMPTS_PREFIX = "bricopro:login:attempts:";
    private static final String LOCKED_PREFIX   = "bricopro:login:locked:";

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Login Succeeded.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public void loginSucceeded(String key) {
        try {
            redisTemplate.delete(ATTEMPTS_PREFIX + key);
            redisTemplate.delete(LOCKED_PREFIX + key);
        } catch (Exception e) {
            log.error("Redis loginSucceeded error: {}", e.getMessage());
        }
    }

    /**
     * Login Failed.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public void loginFailed(String key) {
        try {
            String attemptsKey = ATTEMPTS_PREFIX + key;
            Long count = redisTemplate.opsForValue().increment(attemptsKey);
            
            if (count != null && count == 1) {
                redisTemplate.expire(attemptsKey, Duration.ofMinutes(LOCK_MINUTES * 2L));
            }
            if (count != null && count >= MAX_ATTEMPTS) {
                redisTemplate.opsForValue().set(
                        LOCKED_PREFIX + key, "1",
                        Duration.ofMinutes(LOCK_MINUTES));
            }
        } catch (Exception e) {
            log.error("Redis loginFailed error: {}", e.getMessage());
        }
    }

    /**
     * Is Blocked.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public boolean isBlocked(String key) {
        try {
            return redisTemplate.hasKey(LOCKED_PREFIX + key);
        } catch (Exception e) {
            log.error("Redis isBlocked check failed — not blocking: {}", e.getMessage());
            return false;   
        }
    }

    /**
     * Get Remaining Attempts.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public int getRemainingAttempts(String key) {
        try {
            String val = redisTemplate.opsForValue().get(ATTEMPTS_PREFIX + key);
            int used = val != null ? Integer.parseInt(val) : 0;
            return Math.max(0, MAX_ATTEMPTS - used);
        } catch (Exception e) {
            return MAX_ATTEMPTS;
        }
    }

    /**
     * Get Lock Remaining Seconds.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public long getLockRemainingSeconds(String key) {
        try {
            Long ttl = redisTemplate.getExpire(LOCKED_PREFIX + key,
                    java.util.concurrent.TimeUnit.SECONDS);
            return ttl != null && ttl > 0 ? ttl : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
