package com.bricopro.security;

import com.bricopro.security.oauth2.OAuth2ExchangeCodeService;
import com.bricopro.security.oauth2.OAuth2ExchangeCodeService.ExchangePayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2ExchangeCodeService")
class OAuth2ExchangeCodeServiceTest {

    @Mock RedisTemplate<String, String> redisTemplate;
    @Mock ValueOperations<String, String> valueOperations;

    @InjectMocks OAuth2ExchangeCodeService exchangeCodeService;

    private final ObjectMapper realMapper = new ObjectMapper();

    @BeforeEach
    void setup() throws Exception {
        var field = OAuth2ExchangeCodeService.class.getDeclaredField("objectMapper");
        field.setAccessible(true);
        field.set(exchangeCodeService, realMapper);
    }

    @Test
    @DisplayName("REGRESSION: store() puts real tokens behind a code, never in a URL")
    void storeReturnsOpaqueCode() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        ExchangePayload payload = new ExchangePayload("access-tok", "refresh-tok", 1L, "CLIENT");
        String code = exchangeCodeService.store(payload);

        assertThat(code).isNotNull().isNotEmpty();
        assertThat(code).doesNotContain("access-tok", "refresh-tok");

        verify(valueOperations).set(startsWith("bricopro:oauth2exchange:"), contains("access-tok"), eq(Duration.ofSeconds(30)));
    }

    @Test
    @DisplayName("consume() returns the stored payload and deletes the key (single-use)")
    void consumeReturnsPayloadAndDeletesKey() throws Exception {
        String json = realMapper.writeValueAsString(new ExchangePayload("acc", "ref", 2L, "WORKER"));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("bricopro:oauth2exchange:some-code")).thenReturn(json);

        Optional<ExchangePayload> result = exchangeCodeService.consume("some-code");

        assertThat(result).isPresent();
        assertThat(result.get().accessToken()).isEqualTo("acc");
        assertThat(result.get().userId()).isEqualTo(2L);
        verify(redisTemplate).delete("bricopro:oauth2exchange:some-code");
    }

    @Test
    @DisplayName("REGRESSION: consume() returns empty for an unknown or already-used code, rather than throwing")
    void consumeReturnsEmptyForUnknownCode() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("bricopro:oauth2exchange:expired-or-reused")).thenReturn(null);

        Optional<ExchangePayload> result = exchangeCodeService.consume("expired-or-reused");

        assertThat(result).isEmpty();
        verify(redisTemplate, never()).delete(anyString());
    }
}
