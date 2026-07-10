package com.bricopro.bidding.scheduler;

import com.bricopro.bidding.service.IBiddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BidExpirationScheduler {

    private final IBiddingService biddingService;

    @Scheduled(cron = "0 0 * * * *")
    public void expireBids() {
        biddingService.expirePendingBids();
    }
}