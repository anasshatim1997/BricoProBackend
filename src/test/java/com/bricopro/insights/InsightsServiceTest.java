package com.bricopro.insights;

import com.bricopro.home.dto.CityInsightsDto;
import com.bricopro.user.entity.ClientProfile;
import com.bricopro.user.entity.User;
import com.bricopro.user.repository.ClientProfileRepository;
import com.bricopro.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InsightsService")
class InsightsServiceTest {

    @Mock UserRepository userRepository;
    @Mock ClientProfileRepository clientProfileRepository;
    @Mock CityInsightsCacheService cityInsightsCacheService;

    @InjectMocks InsightsService insightsService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(String email) {
        SecurityContext context = mock(SecurityContext.class);
        var auth = new UsernamePasswordAuthenticationToken(email, null);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
    }

    @Test
    @DisplayName("uses the explicitly given city without touching the current user")
    void usesExplicitCity() {
        CityInsightsDto expected = CityInsightsDto.builder().city("Rabat").build();
        when(cityInsightsCacheService.getCityInsightsCached("Rabat")).thenReturn(expected);

        CityInsightsDto result = insightsService.getCityInsights("Rabat");

        assertThat(result.getCity()).isEqualTo("Rabat");
        verifyNoInteractions(userRepository, clientProfileRepository);
    }

    @Test
    @DisplayName("resolves the city from the current user's profile when none is given")
    void resolvesFromCurrentUserProfile() {
        loginAs("amina@test.ma");
        User amina = User.builder().id(1L).email("amina@test.ma").build();
        when(userRepository.findByEmail("amina@test.ma")).thenReturn(Optional.of(amina));
        when(clientProfileRepository.findByUserId(1L))
                .thenReturn(Optional.of(ClientProfile.builder().city("Marrakech").build()));

        CityInsightsDto expected = CityInsightsDto.builder().city("Marrakech").build();
        when(cityInsightsCacheService.getCityInsightsCached("Marrakech")).thenReturn(expected);

        CityInsightsDto result = insightsService.getCityInsights(null);

        assertThat(result.getCity()).isEqualTo("Marrakech");
        verify(cityInsightsCacheService).getCityInsightsCached("Marrakech");
    }

    @Test
    @DisplayName("defaults to Casablanca when the user has no client profile")
    void defaultsToCasablancaWithNoProfile() {
        loginAs("noprof@test.ma");
        User user = User.builder().id(2L).email("noprof@test.ma").build();
        when(userRepository.findByEmail("noprof@test.ma")).thenReturn(Optional.of(user));
        when(clientProfileRepository.findByUserId(2L)).thenReturn(Optional.empty());

        CityInsightsDto expected = CityInsightsDto.builder().city("Casablanca").build();
        when(cityInsightsCacheService.getCityInsightsCached("Casablanca")).thenReturn(expected);

        insightsService.getCityInsights(null);

        verify(cityInsightsCacheService).getCityInsightsCached("Casablanca");
    }

    @Test
    @DisplayName("REGRESSION: two different users omitting city each get their own resolved city, never a shared null-keyed result")
    void twoUsersOmittingCityGetDifferentResults() {
        loginAs("amina@test.ma");
        User amina = User.builder().id(1L).email("amina@test.ma").build();
        when(userRepository.findByEmail("amina@test.ma")).thenReturn(Optional.of(amina));
        when(clientProfileRepository.findByUserId(1L))
                .thenReturn(Optional.of(ClientProfile.builder().city("Marrakech").build()));
        when(cityInsightsCacheService.getCityInsightsCached("Marrakech"))
                .thenReturn(CityInsightsDto.builder().city("Marrakech").activeWorkers(50L).build());

        CityInsightsDto resultForAmina = insightsService.getCityInsights(null);

        loginAs("youssef@test.ma");
        User youssef = User.builder().id(2L).email("youssef@test.ma").build();
        when(userRepository.findByEmail("youssef@test.ma")).thenReturn(Optional.of(youssef));
        when(clientProfileRepository.findByUserId(2L))
                .thenReturn(Optional.of(ClientProfile.builder().city("Tanger").build()));
        when(cityInsightsCacheService.getCityInsightsCached("Tanger"))
                .thenReturn(CityInsightsDto.builder().city("Tanger").activeWorkers(12L).build());

        CityInsightsDto resultForYoussef = insightsService.getCityInsights(null);

        assertThat(resultForAmina.getCity()).isEqualTo("Marrakech");
        assertThat(resultForYoussef.getCity()).isEqualTo("Tanger");
        assertThat(resultForYoussef.getCity()).isNotEqualTo(resultForAmina.getCity());

        verify(cityInsightsCacheService).getCityInsightsCached("Marrakech");
        verify(cityInsightsCacheService).getCityInsightsCached("Tanger");
        verify(cityInsightsCacheService, never()).getCityInsightsCached(isNull());
    }
}
