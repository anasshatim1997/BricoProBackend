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
public class RecommendationDto {
    private List<ServiceDto> recommendedServices;
    private List<RecentWorkerDto> recommendedWorkers;
}