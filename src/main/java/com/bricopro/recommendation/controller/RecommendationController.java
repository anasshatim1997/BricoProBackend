package com.bricopro.recommendation.controller;

import com.bricopro.home.dto.RecommendationDto;
import com.bricopro.recommendation.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clients/me")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping("/recommendations")
    public RecommendationDto getRecommendations() {
        return recommendationService.getRecommendationsForClient();
    }
}