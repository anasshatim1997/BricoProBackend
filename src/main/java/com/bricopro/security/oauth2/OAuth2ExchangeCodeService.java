package com.bricopro.security.oauth2;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2ExchangeCodeService {

    private static final String PREFIX = "bricopro:oauth2exchange:";
    private static final Duration TTL = Duration.ofSeconds(30);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public String store(ExchangePayload payload) {
        String code = UUID.randomUUID().toString();
        try {
            String json = objectMapper.writeValueAsString(payload);
            redisTemplate.opsForValue().set(PREFIX + code, json, TTL);
        } catch (Exception e) {
            log.error("Failed to store OAuth2 exchange payload in Redis: {}", e.getMessage());
            throw new IllegalStateException("Could not complete social login, please try again");
        }
        return code;
    }

    public Optional<ExchangePayload> consume(String code) {
        String key = PREFIX + code;
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return Optional.empty();
            }
            redisTemplate.delete(key);
            return Optional.of(objectMapper.readValue(json, ExchangePayload.class));
        } catch (Exception e) {
            log.error("Failed to consume OAuth2 exchange code: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public record ExchangePayload(String accessToken, String refreshToken, Long userId, String role) {}
}
