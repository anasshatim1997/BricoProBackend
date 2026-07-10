package com.bricopro.home.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDto {
    private String key;
    private String fr;
    private String ar;
    private String icon;
    private String color;
    private Integer priceMin;
    private Integer priceMax;
}