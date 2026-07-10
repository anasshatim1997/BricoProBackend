package com.bricopro.security;

import com.bricopro.common.sanitizer.HtmlSanitizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("HtmlSanitizer")
class HtmlSanitizerTest {

    private HtmlSanitizer sanitizer;

    @BeforeEach
    void setup() {
        sanitizer = new HtmlSanitizer();
    }

    // ─── sanitize() ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("sanitize()")
    class Sanitize {

        @Test
        @DisplayName("passes through clean text unchanged")
        void cleanText() {
            String result = sanitizer.sanitize("Bonjour, je cherche un plombier.");
            assertThat(result).isEqualTo("Bonjour, je cherche un plombier.");
        }

        @Test
        @DisplayName("removes <script> tags and their content")
        void removesScriptTags() {
            String result = sanitizer.sanitize("<script>alert('XSS')</script>text");
            assertThat(result).doesNotContain("<script>").doesNotContain("alert");
            assertThat(result).contains("text");
        }

        @Test
        @DisplayName("removes all HTML tags")
        void removesHtmlTags() {
            String result = sanitizer.sanitize("<b>bold</b> <em>italic</em>");
            assertThat(result).isEqualTo("bold italic");
        }

        @Test
        @DisplayName("removes inline JavaScript event handlers")
        void removesJsEventHandlers() {
            String result = sanitizer.sanitize("<img onerror='alert(1)' src='x'>");
            assertThat(result).doesNotContain("onerror");
            assertThat(result).doesNotContain("alert");
        }

        @Test
        @DisplayName("removes javascript: URI scheme")
        void removesJavascriptUri() {
            String result = sanitizer.sanitize("<a href='javascript:alert(1)'>click</a>");
            assertThat(result).doesNotContain("javascript:");
        }

        @Test
        @DisplayName("encodes & as &amp;")
        void encodesAmpersand() {
            String result = sanitizer.sanitize("Tom & Jerry");
            assertThat(result).contains("&amp;");
        }

        @Test
        @DisplayName("encodes \" as &quot;")
        void encodesDoubleQuote() {
            String result = sanitizer.sanitize("Say \"hello\"");
            assertThat(result).contains("&quot;");
        }

        @Test
        @DisplayName("encodes ' as &#x27;")
        void encodesSingleQuote() {
            String result = sanitizer.sanitize("don't");
            assertThat(result).contains("&#x27;");
        }

        @Test
        @DisplayName("handles null input — returns null")
        void nullInput() {
            assertThat(sanitizer.sanitize(null)).isNull();
        }

        @Test
        @DisplayName("handles empty string — returns empty")
        void emptyInput() {
            assertThat(sanitizer.sanitize("")).isEqualTo("");
        }

        @Test
        @DisplayName("removes case-insensitive <SCRIPT> tags")
        void caseInsensitiveScript() {
            String result = sanitizer.sanitize("<SCRIPT>evil()</SCRIPT>text");
            assertThat(result).doesNotContain("SCRIPT").doesNotContain("evil");
        }

        @Test
        @DisplayName("trims leading and trailing whitespace")
        void trimsWhitespace() {
            String result = sanitizer.sanitize("  hello  ");
            assertThat(result).isEqualTo("hello");
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "<script>alert(1)</script>",
            "<img src=x onerror=alert(1)>",
            "<svg onload=alert(1)>",
            "<body onload=alert(1)>",
            "javascript:alert('XSS')"
        })
        @DisplayName("blocks common XSS vectors")
        void blocksXssVectors(String xssPayload) {
            String result = sanitizer.sanitize(xssPayload);
            assertThat(result)
                    .doesNotContain("alert")
                    .doesNotContain("<script")
                    .doesNotContain("javascript:");
        }
    }

    // ─── sanitizeLoose() ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("sanitizeLoose()")
    class SanitizeLoose {

        @Test
        @DisplayName("removes script tags but preserves other HTML")
        void removesScriptPreservesHtml() {
            String result = sanitizer.sanitizeLoose("<b>bold</b><script>evil()</script>");
            assertThat(result).contains("<b>bold</b>");
            assertThat(result).doesNotContain("<script>");
        }

        @Test
        @DisplayName("handles null — returns null")
        void nullInput() {
            assertThat(sanitizer.sanitizeLoose(null)).isNull();
        }

        @Test
        @DisplayName("trims result")
        void trimsResult() {
            assertThat(sanitizer.sanitizeLoose("  text  ")).isEqualTo("text");
        }
    }

    // ─── containsSqlInjection() ───────────────────────────────────────────────

    @Nested
    @DisplayName("containsSqlInjection()")
    class SqlInjection {

        @ParameterizedTest
        @ValueSource(strings = {
            "SELECT * FROM users",
            "1'; DROP TABLE tasks;--",
            "1 UNION SELECT password FROM users",
            "admin'--",
            "1; DELETE FROM payments",
            "EXEC xp_cmdshell('dir')",
            "'; INSERT INTO users VALUES ('hacker')"
        })
        @DisplayName("detects SQL injection patterns")
        void detectsSqlInjection(String payload) {
            assertThat(sanitizer.containsSqlInjection(payload)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "Fix the electricity in bedroom",
            "Appartement 3 pièces à Casablanca",
            "Nettoyage complet de la villa",
            "Réparer la robinetterie",
            "Peindre 2 chambres"
        })
        @DisplayName("accepts clean service descriptions")
        void acceptsCleanInput(String clean) {
            assertThat(sanitizer.containsSqlInjection(clean)).isFalse();
        }

        @Test
        @DisplayName("returns false for null input")
        void nullInputReturnsFalse() {
            assertThat(sanitizer.containsSqlInjection(null)).isFalse();
        }

        @Test
        @DisplayName("is case-insensitive for SQL keywords")
        void caseInsensitive() {
            assertThat(sanitizer.containsSqlInjection("select * from users")).isTrue();
            assertThat(sanitizer.containsSqlInjection("Select * From users")).isTrue();
        }
    }
}
