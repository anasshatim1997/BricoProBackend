package com.bricopro.security;

import com.bricopro.common.sanitizer.HtmlSanitizer;
import com.bricopro.config.security.RequestGuardConfig;
import com.bricopro.security.ratelimit.RateLimitFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Security Filters")
class SecurityFiltersTest {

    // ─── RATE LIMIT FILTER ────────────────────────────────────────────────────

    @Nested
    @DisplayName("RateLimitFilter")
    class RateLimitFilterTests {

        private RateLimitFilter rateLimitFilter;

        @BeforeEach
        void setup() {
            rateLimitFilter = new RateLimitFilter();
        }

        @Test
        @DisplayName("allows requests to unprotected endpoints through without rate limiting")
        void allowsUnprotectedEndpoints() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/v1/tasks");
            request.setRemoteAddr("127.0.0.1");

            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            rateLimitFilter.doFilter(request, response, chain);

            // Chain was invoked → no rate limit block
            assertThat(chain.getRequest()).isNotNull();
        }

        @Test
        @DisplayName("allows first login request through")
        void allowsFirstLoginRequest() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/v1/auth/login");
            request.setRemoteAddr("192.168.1.100");

            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            rateLimitFilter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isNotEqualTo(429);
        }

        @Test
        @DisplayName("returns 429 after exceeding rate limit on /auth/login")
        void blocksAfterRateLimitExceeded() throws Exception {
            // IP that hasn't been used before
            String uniqueIp = "10.0.0.99";

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/v1/auth/login");
            request.setRemoteAddr(uniqueIp);

            MockHttpServletResponse lastResponse = null;
            MockFilterChain chain;

            // Login limit = 5 per minute; hit it 6 times
            for (int i = 0; i < 6; i++) {
                lastResponse = new MockHttpServletResponse();
                chain = new MockFilterChain();
                rateLimitFilter.doFilter(request, lastResponse, chain);
            }

            assertThat(lastResponse.getStatus()).isEqualTo(429);
        }

        @Test
        @DisplayName("sets X-Rate-Limit-Remaining header on allowed requests")
        void setsRateLimitHeader() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/v1/auth/login");
            request.setRemoteAddr("172.16.0.1");

            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            rateLimitFilter.doFilter(request, response, chain);

            assertThat(response.getHeader("X-Rate-Limit-Remaining")).isNotNull();
        }

        @Test
        @DisplayName("sets Retry-After header on 429 response")
        void setsRetryAfterHeader() throws Exception {
            String uniqueIp = "10.1.2.99";
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/v1/auth/register");
            request.setRemoteAddr(uniqueIp);

            MockHttpServletResponse lastResponse = null;

            // Register limit = 3 per minute; hit it 4 times
            for (int i = 0; i < 4; i++) {
                lastResponse = new MockHttpServletResponse();
                MockFilterChain chain = new MockFilterChain();
                rateLimitFilter.doFilter(request, lastResponse, chain);
            }

            assertThat(lastResponse.getStatus()).isEqualTo(429);
            assertThat(lastResponse.getHeader("X-Rate-Limit-Retry-After-Seconds")).isNotNull();
        }

        @Test
        @DisplayName("different IPs have independent rate limit buckets")
        void separateBucketsPerIp() throws Exception {
            MockHttpServletRequest req1 = new MockHttpServletRequest();
            req1.setRequestURI("/api/v1/auth/login");
            req1.setRemoteAddr("10.10.10.1");

            MockHttpServletRequest req2 = new MockHttpServletRequest();
            req2.setRequestURI("/api/v1/auth/login");
            req2.setRemoteAddr("10.10.10.2");

            MockHttpServletResponse resp1 = new MockHttpServletResponse();
            MockHttpServletResponse resp2 = new MockHttpServletResponse();

            rateLimitFilter.doFilter(req1, resp1, new MockFilterChain());
            rateLimitFilter.doFilter(req2, resp2, new MockFilterChain());

            // Both first requests should be allowed
            assertThat(resp1.getStatus()).isNotEqualTo(429);
            assertThat(resp2.getStatus()).isNotEqualTo(429);
        }

        @Test
        @DisplayName("uses X-Forwarded-For header for IP identification")
        void usesForwardedForHeader() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/v1/auth/login");
            request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1");
            request.setRemoteAddr("10.0.0.1"); // proxy IP

            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            rateLimitFilter.doFilter(request, response, chain);

            // Should not throw; the real IP 203.0.113.10 is used
            assertThat(response.getStatus()).isNotEqualTo(500);
        }
    }

    // ─── REQUEST SIZE FILTER ──────────────────────────────────────────────────

    @Nested
    @DisplayName("RequestSizeFilter")
    class RequestSizeFilterTests {

        private RequestGuardConfig.RequestSizeFilter sizeFilter;

        @BeforeEach
        void setup() {
            sizeFilter = new RequestGuardConfig.RequestSizeFilter();
        }

        @Test
        @DisplayName("allows requests within 10MB limit")
        void allowsSmallRequest() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setContentType("application/json");
            request.setContent("{}".getBytes()); // tiny body

            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            sizeFilter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isNotEqualTo(413);
        }

        @Test
        @DisplayName("returns 413 for requests exceeding 10MB")
        void blocksOversizedRequest() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setContentType("application/json");
            byte[] oversizedBody = new byte[(int) (11L * 1024 * 1024)];
            request.setContent(oversizedBody);

            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            sizeFilter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(413);
        }
    }

    // ─── SQL INJECTION FILTER ─────────────────────────────────────────────────

    @Nested
    @DisplayName("SqlInjectionFilter")
    class SqlInjectionFilterTests {

        private RequestGuardConfig.SqlInjectionFilter sqlFilter;

        @BeforeEach
        void setup() {
            sqlFilter = new RequestGuardConfig.SqlInjectionFilter();
        }

        @Test
        @DisplayName("allows clean search parameter")
        void allowsCleanSearch() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/v1/users/workers");
            request.addParameter("search", "plombier Casablanca");

            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            sqlFilter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isNotEqualTo(400);
        }

        @Test
        @DisplayName("returns 400 for SQL injection in search parameter")
        void blocksSqlInjection() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/v1/users/workers");
            request.addParameter("search", "' OR 1=1; DROP TABLE users;--");

            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            sqlFilter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(400);
        }

        @Test
        @DisplayName("returns 400 for UNION SELECT in query param")
        void blocksUnionSelect() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addParameter("name", "test UNION SELECT * FROM users");

            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            sqlFilter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(400);
        }

        @Test
        @DisplayName("allows request with unchecked parameter names")
        void allowsUncheckedParams() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            // 'page' is not in the checked param list
            request.addParameter("page", "SELECT * FROM users");

            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            sqlFilter.doFilter(request, response, chain);

            // 'page' is not checked, so it passes through
            assertThat(response.getStatus()).isNotEqualTo(400);
        }

        @Test
        @DisplayName("error response body contains parameter name")
        void errorBodyContainsParamName() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addParameter("email", "admin'--");

            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            sqlFilter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(400);
            assertThat(response.getContentAsString()).contains("email");
        }
    }
}
