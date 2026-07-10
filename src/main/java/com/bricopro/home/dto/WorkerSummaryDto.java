package com.bricopro.home.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkerSummaryDto {
    private Long id;
    private String name;
    private String photo;
    private Double rating;
    private String service;
}