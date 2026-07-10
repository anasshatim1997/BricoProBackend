package com.bricopro.subscription.dto;

import com.bricopro.user.entity.WorkerProfile.ServiceType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateCampaignRequest {
    private ServiceType serviceType;
    private String targetCity;
    private BigDecimal dailyBudget;
    private BigDecimal costPerClick;
    private int durationDays;
}
