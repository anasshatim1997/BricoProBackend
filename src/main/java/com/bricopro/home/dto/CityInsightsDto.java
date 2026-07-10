package com.bricopro.home.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CityInsightsDto {
    private String city;
    private List<String> trendingServices;
    private Long activeWorkers;
    private Double averageResponseTime;
}