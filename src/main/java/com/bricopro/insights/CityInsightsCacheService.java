package com.bricopro.insights;

import com.bricopro.home.dto.CityInsightsDto;
import com.bricopro.task.repository.TaskRepository;
import com.bricopro.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CityInsightsCacheService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    @Cacheable(value = "cityInsights", key = "#city")
    public CityInsightsDto getCityInsightsCached(String city) {
        List<Object[]> trendingData = taskRepository.findTrendingServicesByCity(city, PageRequest.of(0, 3));
        List<String> trendingServices = trendingData.stream()
                .map(row -> row[0].toString())
                .collect(Collectors.toList());

        Long activeWorkers = userRepository.countActiveWorkersInCity(city);
        Double avgResponse = taskRepository.averageResponseTimeInCity(city);

        return CityInsightsDto.builder()
                .city(city)
                .trendingServices(trendingServices)
                .activeWorkers(activeWorkers)
                .averageResponseTime(avgResponse)
                .build();
    }
}
