package com.bricopro.common;

import com.bricopro.common.sanitizer.HtmlSanitizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("HtmlSanitizer")
class HtmlSanitizerTest {

    private final HtmlSanitizer sanitizer = new HtmlSanitizer();

    @Nested
    @DisplayName("sanitize()")
    class Sanitize {

        @Test
        @DisplayName("returns null for null input")
        void nullInputReturnsNull() {
            assertThat(sanitizer.sanitize(null)).isNull();
        }

        @Test
        @DisplayName("strips script tags entirely, including their content")
        void stripsScriptTags() {
            String result = sanitizer.sanitize("hello <script>alert('xss')</script> world");
            assertThat(result).doesNotContain("<script");
            assertThat(result).doesNotContain("alert");
        }

        @Test
        @DisplayName("strips inline JS event handlers like onerror=")
        void stripsJsEventHandlers() {
            String result = sanitizer.sanitize("<img src=x onerror=alert(1)>");
            assertThat(result).doesNotContainIgnoringCase("onerror=");
        }

        @Test
        @DisplayName("strips javascript: URIs")
        void stripsJavascriptUris() {
            String result = sanitizer.sanitize("javascript:alert(1)");
            assertThat(result).doesNotContainIgnoringCase("javascript:");
        }

        @Test
        @DisplayName("strips any remaining HTML tags after script/event removal")
        void stripsRemainingHtmlTags() {
            String result = sanitizer.sanitize("<b>bold</b> and <i>italic</i>");
            assertThat(result).doesNotContain("<b>", "</b>", "<i>", "</i>");
            assertThat(result).contains("bold", "italic");
        }

        @Test
        @DisplayName("HTML-escapes ampersands, quotes, and slashes")
        void escapesSpecialCharacters() {
            String result = sanitizer.sanitize("Tom & Jerry's \"great\" day/night");
            assertThat(result).contains("&amp;");
            assertThat(result).contains("&#x27;");
            assertThat(result).contains("&quot;");
            assertThat(result).contains("&#x2F;");
        }

        @Test
        @DisplayName("trims leading and trailing whitespace")
        void trimsWhitespace() {
            assertThat(sanitizer.sanitize("  hello  ")).isEqualTo("hello");
        }

        @Test
        @DisplayName("leaves plain, benign text unchanged in substance")
        void leavesPlainTextIntact() {
            String result = sanitizer.sanitize("Fix leaking kitchen pipe");
            assertThat(result).isEqualTo("Fix leaking kitchen pipe");
        }
    }

    @Nested
    @DisplayName("sanitizeLoose()")
    class SanitizeLoose {

        @Test
        @DisplayName("removes script tags but does not HTML-escape or strip other tags")
        void removesOnlyScriptTags() {
            String result = sanitizer.sanitizeLoose("<b>bold</b><script>alert(1)</script>");
            assertThat(result).contains("<b>bold</b>");
            assertThat(result).doesNotContain("<script", "alert(1)");
        }

        @Test
        @DisplayName("returns null for null input")
        void nullInputReturnsNull() {
            assertThat(sanitizer.sanitizeLoose(null)).isNull();
        }
    }

    @Nested
    @DisplayName("containsSqlInjection()")
    class ContainsSqlInjection {

        @Test
        @DisplayName("detects common SQL injection keywords")
        void detectsSqlKeywords() {
            assertThat(sanitizer.containsSqlInjection("'; DROP TABLE users; --")).isTrue();
            assertThat(sanitizer.containsSqlInjection("1 UNION SELECT password FROM users")).isTrue();
        }

        @Test
        @DisplayName("returns false for ordinary text containing none of the flagged patterns")
        void returnsFalseForOrdinaryText() {
            assertThat(sanitizer.containsSqlInjection("Fix leaking kitchen pipe")).isFalse();
        }

        @Test
        @DisplayName("returns false for null input")
        void nullInputReturnsFalse() {
            assertThat(sanitizer.containsSqlInjection(null)).isFalse();
        }

        @Test
        @DisplayName("REGRESSION-DOCUMENTING: this check is never reached for @RequestBody JSON fields (see Bug #33) — this test only proves the pattern-matching itself works when it IS reached")
        void documentsThePatternWorksButIsBypassedForJsonBodies() {
            String maliciousTitle = "Task'; DROP TABLE tasks; --";
            assertThat(sanitizer.containsSqlInjection(maliciousTitle)).isTrue();
        }
    }
}
