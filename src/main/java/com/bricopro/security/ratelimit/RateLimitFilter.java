package com.bricopro.security.ratelimit;

import io.github.bucket4j.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter implements Filter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private static final Map<String, BandwidthConfig> ENDPOINT_LIMITS = Map.of(
        "/api/v1/auth/login",           new BandwidthConfig(5,  Duration.ofMinutes(1)),
        "/api/v1/auth/register",        new BandwidthConfig(3,  Duration.ofMinutes(1)),
        "/api/v1/auth/forgot-password", new BandwidthConfig(3,  Duration.ofMinutes(5)),
        "/api/v1/auth/verify-otp",      new BandwidthConfig(5,  Duration.ofMinutes(5)),
        "/api/v1/auth/reset-password",  new BandwidthConfig(3,  Duration.ofMinutes(5)),
        "/api/v1/auth/refresh",         new BandwidthConfig(10, Duration.ofMinutes(1))
    );

    record BandwidthConfig(long tokens, Duration refillPeriod) {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  req = (HttpServletRequest)  request;
        HttpServletResponse res = (HttpServletResponse) response;

        String path = req.getRequestURI();
        BandwidthConfig config = ENDPOINT_LIMITS.get(path);

        if (config == null) {
            chain.doFilter(request, response);
            return;
        }

        String key = getClientKey(req) + ":" + path;
        Bucket bucket = buckets.computeIfAbsent(key, k -> buildBucket(config));

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            res.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            chain.doFilter(request, response);
        } else {
            long retryAfter = probe.getNanosToWaitForRefill() / 1_000_000_000;
            res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            res.setHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(retryAfter));
            res.setContentType("application/json");
            res.getWriter().write("{\"error\":\"Too many requests\",\"retryAfterSeconds\":" + retryAfter + "}");
        }
    }

    private Bucket buildBucket(BandwidthConfig config) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(config.tokens())
                        .refillGreedy(config.tokens(), config.refillPeriod())
                        .build())
                .build();
    }

    private String getClientKey(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        return (forwarded != null) ? forwarded.split(",")[0].trim() : req.getRemoteAddr();
    }
}
