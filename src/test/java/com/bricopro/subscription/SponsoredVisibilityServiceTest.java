package com.bricopro.subscription;

import com.bricopro.subscription.entity.SponsoredWorker;
import com.bricopro.subscription.repository.SponsoredClickRepository;
import com.bricopro.subscription.repository.SponsoredWorkerRepository;
import com.bricopro.subscription.service.SponsoredVisibilityService;
import com.bricopro.user.entity.User;
import com.bricopro.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SponsoredVisibilityService")
class SponsoredVisibilityServiceTest {

    @Mock SponsoredWorkerRepository sponsoredRepo;
    @Mock SponsoredClickRepository clickRepo;
    @Mock UserRepository userRepository;

    @InjectMocks SponsoredVisibilityService sponsoredService;

    private SponsoredWorker campaign;

    @BeforeEach
    void setup() {
        campaign = SponsoredWorker.builder()
                .id(1L)
                .worker(User.builder().id(2L).build())
                .dailyBudget(BigDecimal.valueOf(100))
                .spent(BigDecimal.ZERO)
                .costPerClick(BigDecimal.valueOf(10))
                .clicks(0)
                .active(true)
                .build();
    }

    @Nested
    @DisplayName("recordClick() — the fixed fraud-prevention flow")
    class RecordClick {

        @Test
        @DisplayName("counts a genuinely new click from a given viewer")
        void countsNewClick() {
            when(clickRepo.existsBySponsoredWorkerIdAndViewerId(1L, 50L)).thenReturn(false);
            when(clickRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(sponsoredRepo.findById(1L)).thenReturn(Optional.of(campaign));
            when(sponsoredRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            boolean result = sponsoredService.recordClick(1L, 50L);

            assertThat(result).isTrue();
            assertThat(campaign.getClicks()).isEqualTo(1);
            assertThat(campaign.getSpent()).isEqualByComparingTo(BigDecimal.valueOf(10));
        }

        @Test
        @DisplayName("REGRESSION: rejects a second click from the same viewer on the same campaign")
        void rejectsDuplicateClickFromSameViewer() {
            when(clickRepo.existsBySponsoredWorkerIdAndViewerId(1L, 50L)).thenReturn(true);

            boolean result = sponsoredService.recordClick(1L, 50L);

            assertThat(result).isFalse();
            verify(sponsoredRepo, never()).findById(any());
            verify(sponsoredRepo, never()).save(any());
        }

        @Test
        @DisplayName("REGRESSION: repeated calls from the same attacker cannot drain the budget beyond one real click")
        void repeatedCallsCannotDrainBudget() {
            when(clickRepo.existsBySponsoredWorkerIdAndViewerId(1L, 999L))
                    .thenReturn(false)
                    .thenReturn(true);
            when(clickRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(sponsoredRepo.findById(1L)).thenReturn(Optional.of(campaign));
            when(sponsoredRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            boolean firstAttempt = sponsoredService.recordClick(1L, 999L);
            boolean secondAttempt = sponsoredService.recordClick(1L, 999L);
            boolean thirdAttempt = sponsoredService.recordClick(1L, 999L);

            assertThat(firstAttempt).isTrue();
            assertThat(secondAttempt).isFalse();
            assertThat(thirdAttempt).isFalse();
            assertThat(campaign.getClicks()).isEqualTo(1);
            assertThat(campaign.getSpent()).isEqualByComparingTo(BigDecimal.valueOf(10));
        }

        @Test
        @DisplayName("deactivates the campaign once spent reaches the daily budget")
        void deactivatesWhenBudgetExhausted() {
            campaign.setSpent(BigDecimal.valueOf(95));

            when(clickRepo.existsBySponsoredWorkerIdAndViewerId(1L, 50L)).thenReturn(false);
            when(clickRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(sponsoredRepo.findById(1L)).thenReturn(Optional.of(campaign));
            when(sponsoredRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            sponsoredService.recordClick(1L, 50L);

            assertThat(campaign.isActive()).isFalse();
        }

        @Test
        @DisplayName("returns false for a nonexistent campaign")
        void returnsFalseForNonexistentCampaign() {
            when(clickRepo.existsBySponsoredWorkerIdAndViewerId(999L, 50L)).thenReturn(false);
            when(clickRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(sponsoredRepo.findById(999L)).thenReturn(Optional.empty());

            boolean result = sponsoredService.recordClick(999L, 50L);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("getCampaignStats()")
    class GetCampaignStats {

        @Test
        @DisplayName("aggregates stats using a targeted query, not a full-table scan")
        void aggregatesFromTargetedQuery() {
            SponsoredWorker c2 = SponsoredWorker.builder()
                    .id(2L).active(false).impressions(50).clicks(5).build();
            campaign.setImpressions(100);
            campaign.setClicks(10);

            when(sponsoredRepo.findByWorkerId(2L)).thenReturn(List.of(campaign, c2));

            var stats = sponsoredService.getCampaignStats(2L);

            assertThat(stats.get("activeCampaigns")).isEqualTo(1L);
            assertThat(stats.get("totalImpressions")).isEqualTo(150L);
            assertThat(stats.get("totalClicks")).isEqualTo(15L);
            verify(sponsoredRepo, never()).findAll();
        }
    }
}
