package com.bricopro.recommendation;

import com.bricopro.analytics.service.AnalyticsService;
import com.bricopro.home.dto.RecentWorkerDto;
import com.bricopro.home.dto.RecommendationDto;
import com.bricopro.home.dto.ServiceDto;
import com.bricopro.service.ServiceCategoryService;
import com.bricopro.user.entity.User;
import com.bricopro.user.repository.UserRepository;
import com.bricopro.user.service.RecentWorkersService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final AnalyticsService analyticsService;
    private final ServiceCategoryService serviceCategoryService;
    private final RecentWorkersService recentWorkersService;
    private final UserRepository userRepository;

    public RecommendationDto getRecommendationsForClient() {
        Long userId = getCurrentUserId();
        List<ServiceDto> recommendedServices = analyticsService.getTopServicesForClient(userId);
        if (recommendedServices.isEmpty()) {
            recommendedServices = serviceCategoryService.getAllActive().stream().limit(3).collect(Collectors.toList());
        }
        List<RecentWorkerDto> recommendedWorkers = recentWorkersService.getRecentWorkersForClient();
        return RecommendationDto.builder()
                .recommendedServices(recommendedServices)
                .recommendedWorkers(recommendedWorkers)
                .build();
    }

    private Long getCurrentUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}