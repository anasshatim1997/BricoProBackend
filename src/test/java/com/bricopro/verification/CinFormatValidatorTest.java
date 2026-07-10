package com.bricopro.verification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("CinFormatValidator")
class CinFormatValidatorTest {

    @Nested
    @DisplayName("extractCinNumber()")
    class ExtractCinNumber {

        @Test
        @DisplayName("returns null for null input")
        void nullInputReturnsNull() {
            assertThat(CinFormatValidator.extractCinNumber(null)).isNull();
        }

        @Test
        @DisplayName("extracts a 2-letter + 6-digit CIN number (upper length boundary, 8 chars)")
        void extractsTwoLetterSixDigit() {
            assertThat(CinFormatValidator.extractCinNumber("ROYAUME DU MAROC AB123456"))
                    .isEqualTo("AB123456");
        }

        @Test
        @DisplayName("extracts a 1-letter + 5-digit CIN number (lower length boundary, 6 chars)")
        void extractsOneLetterFiveDigit() {
            assertThat(CinFormatValidator.extractCinNumber("CARTE NATIONALE A12345"))
                    .isEqualTo("A12345");
        }

        @Test
        @DisplayName("returns null when no candidate matches the pattern at all")
        void returnsNullForUnreadableText() {
            assertThat(CinFormatValidator.extractCinNumber("blurry unreadable scan text"))
                    .isNull();
        }

        @Test
        @DisplayName("returns null for a lone letter with no digits")
        void returnsNullForLettersOnly() {
            assertThat(CinFormatValidator.extractCinNumber("MOROCCO ABCDEF")).isNull();
        }

        @Test
        @DisplayName("returns null for digits with no leading letter")
        void returnsNullForDigitsOnly() {
            assertThat(CinFormatValidator.extractCinNumber("123456789")).isNull();
        }

        @Test
        @DisplayName("is case-insensitive since input is normalized to uppercase")
        void isCaseInsensitive() {
            assertThat(CinFormatValidator.extractCinNumber("carte nationale ab123456"))
                    .isEqualTo("AB123456");
        }

        @Test
        @DisplayName("returns the first valid candidate when multiple number-like strings are present")
        void returnsFirstValidCandidate() {
            String result = CinFormatValidator.extractCinNumber("ROYAUME DU MAROC AB123456 valid until 2030");
            assertThat(result).isEqualTo("AB123456");
        }

        @Test
        @DisplayName("ignores non-alphanumeric noise around the number, but a separator INSIDE the number breaks the match")
        void separatorInsideNumberBreaksMatch() {
            assertThat(CinFormatValidator.extractCinNumber("N°: AB123456 (valide)"))
                    .isEqualTo("AB123456");
            // Verified behavior, not a bug fix target here: normalization turns any
            // non-alphanumeric character into a space, so "AB-123456" becomes "AB 123456" —
            // two separate tokens neither of which matches the pattern on its own.
            assertThat(CinFormatValidator.extractCinNumber("N°: AB-123456 (valide)"))
                    .isNull();
        }
    }

    @Nested
    @DisplayName("looksLikeMoroccanId()")
    class LooksLikeMoroccanId {

        @Test
        @DisplayName("returns false for null input")
        void nullInputReturnsFalse() {
            assertThat(CinFormatValidator.looksLikeMoroccanId(null)).isFalse();
        }

        @Test
        @DisplayName("recognizes 'ROYAUME DU MAROC'")
        void recognizesRoyaumeDuMaroc() {
            assertThat(CinFormatValidator.looksLikeMoroccanId("ROYAUME DU MAROC AB123456")).isTrue();
        }

        @Test
        @DisplayName("recognizes 'CARTE NATIONALE'")
        void recognizesCarteNationale() {
            assertThat(CinFormatValidator.looksLikeMoroccanId("CARTE NATIONALE D'IDENTITE")).isTrue();
        }

        @Test
        @DisplayName("recognizes the English 'KINGDOM OF MOROCCO' variant")
        void recognizesEnglishVariant() {
            assertThat(CinFormatValidator.looksLikeMoroccanId("KINGDOM OF MOROCCO ID CARD")).isTrue();
        }

        @Test
        @DisplayName("returns false when no recognized keyword is present, even with a valid-looking number")
        void returnsFalseWithoutKeyword() {
            assertThat(CinFormatValidator.looksLikeMoroccanId("AB123456")).isFalse();
        }

        @Test
        @DisplayName("is case-insensitive")
        void isCaseInsensitive() {
            assertThat(CinFormatValidator.looksLikeMoroccanId("royaume du maroc")).isTrue();
        }
    }
}
