package com.bricopro.recommendation;

import com.bricopro.analytics.service.AnalyticsService;
import com.bricopro.home.dto.RecentWorkerDto;
import com.bricopro.home.dto.RecommendationDto;
import com.bricopro.home.dto.ServiceDto;
import com.bricopro.service.ServiceCategoryService;
import com.bricopro.user.entity.User;
import com.bricopro.user.repository.UserRepository;
import com.bricopro.user.service.RecentWorkersService;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationService")
class RecommendationServiceTest {

    @Mock AnalyticsService analyticsService;
    @Mock ServiceCategoryService serviceCategoryService;
    @Mock RecentWorkersService recentWorkersService;
    @Mock UserRepository userRepository;

    @InjectMocks RecommendationService recommendationService;

    private User client;

    @BeforeEach
    void setup() {
        client = User.builder().id(3L).email("client@test.ma").build();

        SecurityContext context = mock(SecurityContext.class);
        var auth = new UsernamePasswordAuthenticationToken("client@test.ma", null);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        when(userRepository.findByEmail("client@test.ma")).thenReturn(Optional.of(client));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("uses analytics-based recommendations when available")
    void usesAnalyticsRecommendationsWhenAvailable() {
        ServiceDto plumbing = ServiceDto.builder().key("PLUMBING").build();
        when(analyticsService.getTopServicesForClient(3L)).thenReturn(List.of(plumbing));
        when(recentWorkersService.getRecentWorkersForClient()).thenReturn(List.of());

        RecommendationDto result = recommendationService.getRecommendationsForClient();

        assertThat(result.getRecommendedServices()).containsExactly(plumbing);
        verify(serviceCategoryService, never()).getAllActive();
    }

    @Test
    @DisplayName("falls back to top 3 active categories when analytics returns nothing")
    void fallsBackToActiveCategoriesWhenAnalyticsEmpty() {
        ServiceDto s1 = ServiceDto.builder().key("A").build();
        ServiceDto s2 = ServiceDto.builder().key("B").build();
        ServiceDto s3 = ServiceDto.builder().key("C").build();
        ServiceDto s4 = ServiceDto.builder().key("D").build();

        when(analyticsService.getTopServicesForClient(3L)).thenReturn(List.of());
        when(serviceCategoryService.getAllActive()).thenReturn(List.of(s1, s2, s3, s4));
        when(recentWorkersService.getRecentWorkersForClient()).thenReturn(List.of());

        RecommendationDto result = recommendationService.getRecommendationsForClient();

        assertThat(result.getRecommendedServices()).hasSize(3);
        assertThat(result.getRecommendedServices()).containsExactly(s1, s2, s3);
    }

    @Test
    @DisplayName("includes recently-worked-with workers in the response")
    void includesRecentWorkers() {
        RecentWorkerDto worker = RecentWorkerDto.builder().build();
        when(analyticsService.getTopServicesForClient(3L)).thenReturn(List.of());
        when(serviceCategoryService.getAllActive()).thenReturn(List.of());
        when(recentWorkersService.getRecentWorkersForClient()).thenReturn(List.of(worker));

        RecommendationDto result = recommendationService.getRecommendationsForClient();

        assertThat(result.getRecommendedWorkers()).containsExactly(worker);
    }
}
