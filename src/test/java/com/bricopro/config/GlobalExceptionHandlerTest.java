package com.bricopro.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Nested
    @DisplayName("handleIllegalArg()")
    class HandleIllegalArg {

        @Test
        @DisplayName("maps to 400 Bad Request with the exception message")
        void mapsTo400() {
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> result =
                    handler.handleIllegalArg(new IllegalArgumentException("Task must have an agreed price"));

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(result.getBody().status()).isEqualTo(400);
            assertThat(result.getBody().error()).isEqualTo("Bad Request");
            assertThat(result.getBody().message()).isEqualTo("Task must have an agreed price");
        }
    }

    @Nested
    @DisplayName("handleIllegalState()")
    class HandleIllegalState {

        @Test
        @DisplayName("maps to 409 Conflict with the exception message")
        void mapsTo409() {
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> result =
                    handler.handleIllegalState(new IllegalStateException("Bid is not pending"));

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(result.getBody().status()).isEqualTo(409);
            assertThat(result.getBody().message()).isEqualTo("Bid is not pending");
        }
    }

    @Nested
    @DisplayName("handleBadCreds()")
    class HandleBadCreds {

        @Test
        @DisplayName("maps to 401 Unauthorized")
        void mapsTo401() {
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> result =
                    handler.handleBadCreds(new BadCredentialsException("Invalid credentials"));

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(result.getBody().status()).isEqualTo(401);
        }
    }

    @Nested
    @DisplayName("handleAccessDenied()")
    class HandleAccessDenied {

        @Test
        @DisplayName("maps to 403 Forbidden")
        void mapsTo403() {
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> result =
                    handler.handleAccessDenied(new AccessDeniedException("Not allowed"));

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(result.getBody().status()).isEqualTo(403);
        }
    }

    @Nested
    @DisplayName("handleGeneric() — the fixed logging behavior from Bug #7")
    class HandleGeneric {

        @Test
        @DisplayName("maps to a generic 500 without leaking the real exception message to the client")
        void mapsTo500WithoutLeakingDetails() {
            ResponseEntity<GlobalExceptionHandler.ErrorResponse> result =
                    handler.handleGeneric(new RuntimeException("Sensitive internal detail: DB password is X"));

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(result.getBody().status()).isEqualTo(500);
            assertThat(result.getBody().message()).isEqualTo("An unexpected error occurred");
            assertThat(result.getBody().message()).doesNotContain("Sensitive internal detail");
        }

        @Test
        @DisplayName("REGRESSION: does not throw while logging, even for an exception with a null message")
        void handlesNullMessageExceptionGracefully() {
            assertThatCode(() -> handler.handleGeneric(new RuntimeException((String) null)))
                    .doesNotThrowAnyException();
        }
    }
}
