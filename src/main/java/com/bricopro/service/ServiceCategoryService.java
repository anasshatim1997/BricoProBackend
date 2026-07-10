package com.bricopro.service;

import com.bricopro.home.dto.ServiceDto;
import com.bricopro.service.entity.ServiceCategory;
import com.bricopro.service.repository.ServiceCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceCategoryService {

    private final ServiceCategoryRepository repository;

    @Transactional(readOnly = true)
    public List<ServiceDto> getAllActive() {
        return repository.findByActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ServiceDto findByKey(String key) {
        return repository.findByKey(key)
                .map(this::toDto)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ServiceDto> searchByName(String query) {
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }
        String q = query.trim();
        return repository.findByFrNameContainingIgnoreCaseOrArNameContainingIgnoreCase(q, q)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private ServiceDto toDto(ServiceCategory sc) {
        return ServiceDto.builder()
                .key(sc.getKey())
                .fr(sc.getFrName())
                .ar(sc.getArName())
                .icon(sc.getIcon())
                .color(sc.getColor())
                .priceMin(sc.getPriceMin())
                .priceMax(sc.getPriceMax())
                .build();
    }
}