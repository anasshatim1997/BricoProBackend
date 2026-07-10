package com.bricopro.home.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentWorkerDto {
    private Long workerId;
    private String name;
    private String photoUrl;
    private Double averageRating;
    private String lastServiceDate;
    private String lastServiceTitle;
}