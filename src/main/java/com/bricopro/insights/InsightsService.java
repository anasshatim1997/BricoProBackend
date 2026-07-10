package com.bricopro.insights;

import com.bricopro.home.dto.CityInsightsDto;
import com.bricopro.user.entity.ClientProfile;
import com.bricopro.user.entity.User;
import com.bricopro.user.repository.ClientProfileRepository;
import com.bricopro.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InsightsService {

    private final UserRepository userRepository;
    private final ClientProfileRepository clientProfileRepository;
    private final CityInsightsCacheService cityInsightsCacheService;

    public CityInsightsDto getCityInsights(String city) {
        String resolvedCity = city != null ? city : resolveCurrentUserCity();
        return cityInsightsCacheService.getCityInsightsCached(resolvedCity);
    }

    private String resolveCurrentUserCity() {
        User user = getCurrentUser();
        ClientProfile profile = clientProfileRepository.findByUserId(user.getId()).orElse(null);
        return profile != null ? profile.getCity() : "Casablanca";
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
