package com.bricopro.referral;

import com.bricopro.notification.service.CommunicationService;
import com.bricopro.referral.entity.ReferralCode;
import com.bricopro.referral.entity.ReferralUse;
import com.bricopro.referral.entity.ReferralUse.RewardStatus;
import com.bricopro.referral.repository.ReferralCodeRepository;
import com.bricopro.referral.repository.ReferralUseRepository;
import com.bricopro.referral.service.ReferralService;
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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReferralService")
class ReferralServiceTest {

    @Mock ReferralCodeRepository referralCodeRepository;
    @Mock ReferralUseRepository  referralUseRepository;
    @Mock UserRepository         userRepository;
    @Mock CommunicationService   communicationService;

    @InjectMocks ReferralService referralService;

    private User referrer;
    private User referred;

    @BeforeEach
    void setup() {
        referrer = User.builder().id(1L).firstName("Sara").email("sara@test.ma").build();
        referred = User.builder().id(2L).firstName("Yassine").email("yassine@test.ma").build();
    }

    @Nested
    @DisplayName("getOrCreateCode()")
    class GetOrCreateCode {

        @Test
        @DisplayName("returns the existing code if one already exists")
        void returnsExisting() {
            ReferralCode existing = ReferralCode.builder().id(1L).user(referrer).code("BRICO1ABCD").build();
            when(referralCodeRepository.findByUserId(1L)).thenReturn(Optional.of(existing));

            ReferralCode result = referralService.getOrCreateCode(1L);

            assertThat(result.getCode()).isEqualTo("BRICO1ABCD");
            verify(referralCodeRepository, never()).save(any());
        }

        @Test
        @DisplayName("generates and persists a new code when none exists")
        void generatesNew() {
            when(referralCodeRepository.findByUserId(1L)).thenReturn(Optional.empty());
            when(userRepository.findById(1L)).thenReturn(Optional.of(referrer));
            when(referralCodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ReferralCode result = referralService.getOrCreateCode(1L);

            assertThat(result.getCode()).startsWith("BRICO1");
            assertThat(result.getTotalRewardsEarned()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("applyReferralCode()")
    class ApplyReferralCode {

        @Test
        @DisplayName("creates a PENDING referral use and does NOT credit totalRewardsEarned yet")
        void appliesWithoutImmediateCredit() {
            ReferralCode referrerCode = ReferralCode.builder()
                    .id(1L).user(referrer).code("BRICO1ABCD")
                    .timesUsed(0).totalRewardsEarned(BigDecimal.ZERO).build();

            when(referralUseRepository.existsByReferredId(2L)).thenReturn(false);
            when(referralCodeRepository.findByCode("BRICO1ABCD")).thenReturn(Optional.of(referrerCode));
            when(userRepository.findById(2L)).thenReturn(Optional.of(referred));
            when(referralUseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(referralCodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Map<String, Object> result = referralService.applyReferralCode(2L, "BRICO1ABCD");

            assertThat(result).containsKey("referrerName");

            var useCaptor = org.mockito.ArgumentCaptor.forClass(ReferralUse.class);
            verify(referralUseRepository).save(useCaptor.capture());
            assertThat(useCaptor.getValue().getRewardStatus()).isEqualTo(RewardStatus.PENDING);

            assertThat(referrerCode.getTotalRewardsEarned()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(referrerCode.getTimesUsed()).isEqualTo(1);
        }

        @Test
        @DisplayName("rejects applying a code twice on the same account")
        void rejectsDoubleApplication() {
            when(referralUseRepository.existsByReferredId(2L)).thenReturn(true);

            assertThatThrownBy(() -> referralService.applyReferralCode(2L, "BRICO1ABCD"))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("rejects an unknown code")
        void rejectsUnknownCode() {
            when(referralUseRepository.existsByReferredId(2L)).thenReturn(false);
            when(referralCodeRepository.findByCode("NOPE")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> referralService.applyReferralCode(2L, "NOPE"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects using your own referral code")
        void rejectsSelfReferral() {
            ReferralCode ownCode = ReferralCode.builder().id(1L).user(referred).code("BRICO2XYZ").build();
            when(referralUseRepository.existsByReferredId(2L)).thenReturn(false);
            when(referralCodeRepository.findByCode("BRICO2XYZ")).thenReturn(Optional.of(ownCode));

            assertThatThrownBy(() -> referralService.applyReferralCode(2L, "BRICO2XYZ"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("creditPendingRewardIfFirstCompletedTask() — the fixed crediting flow")
    class CreditPendingReward {

        @Test
        @DisplayName("credits the referrer's totalRewardsEarned and marks the use CREDITED")
        void creditsSuccessfully() {
            ReferralUse pendingUse = ReferralUse.builder()
                    .id(5L).referrer(referrer).referred(referred)
                    .referrerReward(BigDecimal.valueOf(50))
                    .rewardStatus(RewardStatus.PENDING)
                    .build();

            ReferralCode referrerCode = ReferralCode.builder()
                    .id(1L).user(referrer)
                    .totalRewardsEarned(BigDecimal.ZERO)
                    .build();

            when(referralUseRepository.findByReferredIdAndRewardStatus(2L, RewardStatus.PENDING))
                    .thenReturn(Optional.of(pendingUse));
            when(referralCodeRepository.findByUserId(1L)).thenReturn(Optional.of(referrerCode));
            when(referralUseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(referralCodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            referralService.creditPendingRewardIfFirstCompletedTask(2L);

            assertThat(pendingUse.getRewardStatus()).isEqualTo(RewardStatus.CREDITED);
            assertThat(pendingUse.getCreditedAt()).isNotNull();
            assertThat(referrerCode.getTotalRewardsEarned()).isEqualByComparingTo(BigDecimal.valueOf(50));
            verify(communicationService).sendEmail(eq("sara@test.ma"), anyString(), anyString());
        }

        @Test
        @DisplayName("does nothing when there's no pending reward for this user")
        void noPendingRewardDoesNothing() {
            when(referralUseRepository.findByReferredIdAndRewardStatus(2L, RewardStatus.PENDING))
                    .thenReturn(Optional.empty());

            referralService.creditPendingRewardIfFirstCompletedTask(2L);

            verify(referralUseRepository, never()).save(any());
            verify(referralCodeRepository, never()).save(any());
            verifyNoInteractions(communicationService);
        }
    }

    @Nested
    @DisplayName("getStats()")
    class GetStats {

        @Test
        @DisplayName("returns code, usage count, total earned, and pending count")
        void returnsStats() {
            ReferralCode code = ReferralCode.builder()
                    .id(1L).user(referrer).code("BRICO1ABCD")
                    .totalRewardsEarned(BigDecimal.valueOf(50)).build();

            ReferralUse pending = ReferralUse.builder().rewardStatus(RewardStatus.PENDING).build();
            ReferralUse credited = ReferralUse.builder().rewardStatus(RewardStatus.CREDITED).build();

            when(referralUseRepository.findByReferrerId(1L)).thenReturn(List.of(pending, credited));
            when(referralCodeRepository.findByUserId(1L)).thenReturn(Optional.of(code));

            Map<String, Object> stats = referralService.getStats(1L);

            assertThat(stats.get("timesUsed")).isEqualTo(2);
            assertThat(stats.get("totalEarned")).isEqualTo(BigDecimal.valueOf(50));
            assertThat(stats.get("pendingRewards")).isEqualTo(1L);
        }
    }
}
