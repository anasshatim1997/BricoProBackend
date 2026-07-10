package com.bricopro.preference;

import com.bricopro.preference.dto.UpdatePreferenceRequest;
import com.bricopro.preference.entity.UserPreference;
import com.bricopro.preference.repository.UserPreferenceRepository;
import com.bricopro.preference.service.UserPreferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserPreferenceService")
class UserPreferenceServiceTest {

    @Mock UserPreferenceRepository prefRepository;

    @InjectMocks UserPreferenceService preferenceService;

    @Nested
    @DisplayName("getOrCreate()")
    class GetOrCreate {

        @Test
        @DisplayName("returns the existing preferences when found")
        void returnsExisting() {
            UserPreference existing = UserPreference.builder().userId(1L).language("ar").build();
            when(prefRepository.findById(1L)).thenReturn(Optional.of(existing));

            UserPreference result = preferenceService.getOrCreate(1L);

            assertThat(result.getLanguage()).isEqualTo("ar");
            verify(prefRepository, never()).save(any());
        }

        @Test
        @DisplayName("creates defaults when none exist yet")
        void createsDefaults() {
            when(prefRepository.findById(1L)).thenReturn(Optional.empty());
            when(prefRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UserPreference result = preferenceService.getOrCreate(1L);

            assertThat(result.getLanguage()).isEqualTo("fr");
            assertThat(result.getTheme()).isEqualTo("light");
            assertThat(result.isPushEnabled()).isTrue();
            assertThat(result.isMarketingEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("only overwrites fields that were actually supplied")
        void onlyOverwritesSuppliedFields() {
            UserPreference existing = UserPreference.builder()
                    .userId(1L).language("fr").theme("light")
                    .pushEnabled(true).emailEnabled(true).smsEnabled(true)
                    .marketingEnabled(false).defaultCity("Casablanca")
                    .build();
            when(prefRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(prefRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UpdatePreferenceRequest req = new UpdatePreferenceRequest();
            req.setLanguage("ar");
            req.setMarketingEnabled(true);

            UserPreference result = preferenceService.update(1L, req);

            assertThat(result.getLanguage()).isEqualTo("ar");
            assertThat(result.isMarketingEnabled()).isTrue();
            assertThat(result.getTheme()).isEqualTo("light");
            assertThat(result.getDefaultCity()).isEqualTo("Casablanca");
        }

        @Test
        @DisplayName("creates defaults first if the user had no preferences yet, then applies the update")
        void createsThenUpdatesWhenMissing() {
            when(prefRepository.findById(1L)).thenReturn(Optional.empty());
            when(prefRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UpdatePreferenceRequest req = new UpdatePreferenceRequest();
            req.setDefaultCity("Marrakech");

            UserPreference result = preferenceService.update(1L, req);

            assertThat(result.getDefaultCity()).isEqualTo("Marrakech");
            assertThat(result.getLanguage()).isEqualTo("fr");
        }
    }
}
