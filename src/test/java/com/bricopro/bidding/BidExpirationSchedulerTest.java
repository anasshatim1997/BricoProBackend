package com.bricopro.bidding;

import com.bricopro.bidding.scheduler.BidExpirationScheduler;
import com.bricopro.bidding.service.IBiddingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BidExpirationScheduler")
class BidExpirationSchedulerTest {

    @Mock IBiddingService biddingService;

    @InjectMocks BidExpirationScheduler scheduler;

    @Test
    @DisplayName("delegates to biddingService.expirePendingBids()")
    void delegatesToExpirePendingBids() {
        scheduler.expireBids();

        verify(biddingService).expirePendingBids();
    }
}
