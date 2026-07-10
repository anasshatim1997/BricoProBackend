package com.bricopro.i18n;

import com.bricopro.i18n.controller.I18nController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("I18nController")
class I18nControllerTest {

    private final I18nController controller = new I18nController();

    @Test
    @DisplayName("returns French translations for 'fr'")
    void returnsFrenchTranslations() {
        Map<String, String> result = controller.translations("fr").getBody();

        assertThat(result).isNotNull();
        assertThat(result.get("app.name")).isEqualTo("BricoPro");
        assertThat(result.get("home.title")).isEqualTo("Services à domicile au Maroc");
    }

    @Test
    @DisplayName("returns Arabic translations for 'ar'")
    void returnsArabicTranslations() {
        Map<String, String> result = controller.translations("ar").getBody();

        assertThat(result).isNotNull();
        assertThat(result.get("app.name")).isEqualTo("بريكو برو");
    }

    @Test
    @DisplayName("falls back to French for an unknown language code")
    void fallsBackToFrenchForUnknownLanguage() {
        Map<String, String> result = controller.translations("es").getBody();

        assertThat(result).isNotNull();
        assertThat(result.get("app.name")).isEqualTo("BricoPro");
    }

    @Test
    @DisplayName("French and Arabic translation sets contain the exact same keys")
    void bothLanguagesHaveMatchingKeys() {
        Map<String, String> fr = controller.translations("fr").getBody();
        Map<String, String> ar = controller.translations("ar").getBody();

        assertThat(fr).isNotNull();
        assertThat(ar).isNotNull();
        assertThat(fr.keySet()).isEqualTo(ar.keySet());
    }
}
