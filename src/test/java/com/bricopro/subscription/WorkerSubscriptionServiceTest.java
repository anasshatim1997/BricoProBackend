package com.bricopro.subscription;

import com.bricopro.subscription.entity.WorkerSubscription;
import com.bricopro.subscription.entity.WorkerSubscription.Plan;
import com.bricopro.subscription.entity.WorkerSubscription.SubStatus;
import com.bricopro.subscription.repository.WorkerSubscriptionRepository;
import com.bricopro.subscription.service.WorkerSubscriptionService;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.WorkerProfile;
import com.bricopro.user.repository.UserRepository;
import com.bricopro.user.repository.WorkerProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkerSubscriptionService")
class WorkerSubscriptionServiceTest {

    @Mock WorkerSubscriptionRepository subRepository;
    @Mock WorkerProfileRepository workerProfileRepository;
    @Mock UserRepository userRepository;

    @InjectMocks WorkerSubscriptionService subscriptionService;

    private User worker;

    @BeforeEach
    void setup() {
        worker = User.builder().id(2L).firstName("Karim").build();
    }

    @Nested
    @DisplayName("getCurrentPlan()")
    class GetCurrentPlan {

        @Test
        @DisplayName("returns the active subscription when one exists")
        void returnsActiveSubscription() {
            WorkerSubscription sub = WorkerSubscription.builder().plan(Plan.PREMIUM).build();
            when(subRepository.findTopByWorkerIdAndSubStatusOrderByCreatedAtDesc(2L, SubStatus.ACTIVE))
                    .thenReturn(Optional.of(sub));

            WorkerSubscription result = subscriptionService.getCurrentPlan(2L);

            assertThat(result.getPlan()).isEqualTo(Plan.PREMIUM);
        }

        @Test
        @DisplayName("defaults to FREE when no subscription exists")
        void defaultsToFree() {
            when(subRepository.findTopByWorkerIdAndSubStatusOrderByCreatedAtDesc(2L, SubStatus.ACTIVE))
                    .thenReturn(Optional.empty());

            WorkerSubscription result = subscriptionService.getCurrentPlan(2L);

            assertThat(result.getPlan()).isEqualTo(Plan.FREE);
        }
    }

    @Nested
    @DisplayName("canAcceptJob()")
    class CanAcceptJob {

        @Test
        @DisplayName("FREE plan allows up to 3 jobs per month, blocks the 4th")
        void freeAllowsThreeJobs() {
            when(subRepository.findTopByWorkerIdAndSubStatusOrderByCreatedAtDesc(2L, SubStatus.ACTIVE))
                    .thenReturn(Optional.empty());

            assertThat(subscriptionService.canAcceptJob(2L, 2)).isTrue();
            assertThat(subscriptionService.canAcceptJob(2L, 3)).isFalse();
        }

        @Test
        @DisplayName("PREMIUM plan allows unlimited jobs")
        void premiumAllowsUnlimited() {
            WorkerSubscription sub = WorkerSubscription.builder().plan(Plan.PREMIUM).build();
            when(subRepository.findTopByWorkerIdAndSubStatusOrderByCreatedAtDesc(2L, SubStatus.ACTIVE))
                    .thenReturn(Optional.of(sub));

            assertThat(subscriptionService.canAcceptJob(2L, 500)).isTrue();
        }

        @Test
        @DisplayName("falls back to FREE limits once an active plan has expired")
        void fallsBackToFreeWhenExpired() {
            WorkerSubscription expired = WorkerSubscription.builder()
                    .plan(Plan.PREMIUM)
                    .expiresAt(LocalDateTime.now().minusDays(1))
                    .build();
            when(subRepository.findTopByWorkerIdAndSubStatusOrderByCreatedAtDesc(2L, SubStatus.ACTIVE))
                    .thenReturn(Optional.of(expired));

            assertThat(subscriptionService.canAcceptJob(2L, 3)).isFalse();
        }
    }

    @Nested
    @DisplayName("upgrade()")
    class Upgrade {

        @Test
        @DisplayName("cancels the previous active subscription and creates a new one")
        void cancelsOldCreatesNew() {
            WorkerSubscription oldSub = WorkerSubscription.builder()
                    .id(1L).plan(Plan.FREE).subStatus(SubStatus.ACTIVE).build();

            when(userRepository.findById(2L)).thenReturn(Optional.of(worker));
            when(subRepository.findTopByWorkerIdAndSubStatusOrderByCreatedAtDesc(2L, SubStatus.ACTIVE))
                    .thenReturn(Optional.of(oldSub));
            when(subRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(workerProfileRepository.findByUserId(2L))
                    .thenReturn(Optional.of(WorkerProfile.builder().build()));
            when(workerProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            WorkerSubscription result = subscriptionService.upgrade(2L, Plan.PREMIUM);

            assertThat(oldSub.getSubStatus()).isEqualTo(SubStatus.CANCELLED);
            assertThat(result.getPlan()).isEqualTo(Plan.PREMIUM);
            assertThat(result.getAmountPaid()).isEqualByComparingTo("99");
            assertThat(result.getExpiresAt()).isAfter(LocalDateTime.now());
        }

        @Test
        @DisplayName("marks the worker profile as premium after upgrading")
        void marksProfileAsPremium() {
            when(userRepository.findById(2L)).thenReturn(Optional.of(worker));
            when(subRepository.findTopByWorkerIdAndSubStatusOrderByCreatedAtDesc(2L, SubStatus.ACTIVE))
                    .thenReturn(Optional.empty());
            when(subRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            WorkerProfile profile = WorkerProfile.builder().build();
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(profile));
            when(workerProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            subscriptionService.upgrade(2L, Plan.ENTERPRISE);

            assertThat(profile.isPremium()).isTrue();
        }

        @Test
        @DisplayName("throws when the worker doesn't exist")
        void throwsWhenWorkerMissing() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> subscriptionService.upgrade(999L, Plan.PREMIUM))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("getPlansInfo()")
    class GetPlansInfo {

        @Test
        @DisplayName("returns all three plans with correct prices")
        void returnsAllPlans() {
            var plans = subscriptionService.getPlansInfo();

            assertThat(plans).containsKeys("FREE", "PREMIUM", "ENTERPRISE");
            assertThat(((java.util.Map<?, ?>) plans.get("PREMIUM")).get("price")).isEqualTo(99);
        }
    }
}
