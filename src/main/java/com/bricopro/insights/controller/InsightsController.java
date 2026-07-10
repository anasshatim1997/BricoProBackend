package com.bricopro.insights.controller;

import com.bricopro.home.dto.CityInsightsDto;
import com.bricopro.insights.InsightsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/insights")
@RequiredArgsConstructor
public class InsightsController {

    private final InsightsService insightsService;

    @GetMapping("/city")
    public CityInsightsDto getCityInsights(@RequestParam(required = false) String city) {
        return insightsService.getCityInsights(city);
    }
}