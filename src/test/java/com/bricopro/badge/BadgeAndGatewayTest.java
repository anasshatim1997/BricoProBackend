package com.bricopro.badge;

import com.bricopro.badge.service.BadgeService;
import com.bricopro.badge.entity.WorkerBadge;
import com.bricopro.badge.entity.WorkerBadge.BadgeType;
import com.bricopro.badge.repository.WorkerBadgeRepository;
import com.bricopro.payment.gateway.CashGateway;
import com.bricopro.payment.gateway.PaymentGateway.GatewayResult;
import com.bricopro.task.repository.ReviewRepository;
import com.bricopro.task.repository.TaskRepository;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.User.Role;
import com.bricopro.user.entity.WorkerProfile;
import com.bricopro.user.repository.WorkerProfileRepository;
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
@DisplayName("Badge & Payment Gateway")
class BadgeAndGatewayTest {

    @Nested
    @DisplayName("BadgeService")
    class BadgeServiceTests {

        @Mock WorkerBadgeRepository badgeRepository;
        @Mock WorkerProfileRepository workerProfileRepository;
        @Mock TaskRepository taskRepository;
        @Mock ReviewRepository reviewRepository;

        @InjectMocks BadgeService badgeService;

        private User workerUser;
        private WorkerProfile profile;

        @BeforeEach
        void setup() {
            workerUser = User.builder().id(2L).firstName("Ilyas").lastName("Naciri")
                    .role(Role.WORKER).build();

            profile = WorkerProfile.builder()
                    .id(1L).user(workerUser)
                    .averageRating(BigDecimal.valueOf(4.6))
                    .totalReviews(8).totalMissions(55)
                    .responseRate(BigDecimal.valueOf(92))
                    .cancellationCount(0)
                    .cinVerified(true)
                    .isPremium(false)
                    .build();
        }

        @Test
        @DisplayName("awards NEW_WORKER badge when no missions completed")
        void awardsNewWorkerBadge() {
            profile.setTotalMissions(0);
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(profile));
            when(badgeRepository.existsByUserIdAndBadgeType(2L, BadgeType.NEW_WORKER))
                    .thenReturn(false);
            when(badgeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(badgeRepository.existsByUserIdAndBadgeType(eq(2L),
                    argThat(t -> t != BadgeType.NEW_WORKER)))
                    .thenReturn(true);

            List<WorkerBadge> badges = badgeService.evaluateAndAssign(2L);

            assertThat(badges).anyMatch(b -> b.getBadgeType() == BadgeType.NEW_WORKER);
        }

        @Test
        @DisplayName("awards VERIFIED_CIN badge when CIN is verified")
        void awardsVerifiedCinBadge() {
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(profile));
            when(badgeRepository.existsByUserIdAndBadgeType(2L, BadgeType.VERIFIED_CIN))
                    .thenReturn(false);
            when(badgeRepository.existsByUserIdAndBadgeType(eq(2L),
                    argThat(t -> t != BadgeType.VERIFIED_CIN)))
                    .thenReturn(true);
            when(badgeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            List<WorkerBadge> badges = badgeService.evaluateAndAssign(2L);

            assertThat(badges).anyMatch(b -> b.getBadgeType() == BadgeType.VERIFIED_CIN);
        }

        @Test
        @DisplayName("awards EXPERIENCED badge when >= 50 missions")
        void awardsExperiencedBadge() {
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(profile));
            when(badgeRepository.existsByUserIdAndBadgeType(2L, BadgeType.EXPERIENCED))
                    .thenReturn(false);
            when(badgeRepository.existsByUserIdAndBadgeType(eq(2L),
                    argThat(t -> t != BadgeType.EXPERIENCED)))
                    .thenReturn(true);
            when(badgeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            List<WorkerBadge> badges = badgeService.evaluateAndAssign(2L);

            assertThat(badges).anyMatch(b -> b.getBadgeType() == BadgeType.EXPERIENCED);
        }

        @Test
        @DisplayName("awards TOP_RATED badge when rating >= 4.5 with >= 5 reviews")
        void awardsTopRatedBadge() {
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(profile));
            when(badgeRepository.existsByUserIdAndBadgeType(2L, BadgeType.TOP_RATED))
                    .thenReturn(false);
            when(badgeRepository.existsByUserIdAndBadgeType(eq(2L),
                    argThat(t -> t != BadgeType.TOP_RATED)))
                    .thenReturn(true);
            when(badgeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            List<WorkerBadge> badges = badgeService.evaluateAndAssign(2L);

            assertThat(badges).anyMatch(b -> b.getBadgeType() == BadgeType.TOP_RATED);
        }

        @Test
        @DisplayName("does not award badge already held by worker")
        void doesNotDuplicateBadge() {
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(profile));
            when(badgeRepository.existsByUserIdAndBadgeType(2L, BadgeType.VERIFIED_CIN))
                    .thenReturn(true);
            when(badgeRepository.existsByUserIdAndBadgeType(eq(2L),
                    argThat(t -> t != BadgeType.VERIFIED_CIN)))
                    .thenReturn(true);

            List<WorkerBadge> badges = badgeService.evaluateAndAssign(2L);

            assertThat(badges).noneMatch(b -> b.getBadgeType() == BadgeType.VERIFIED_CIN);
        }

        @Test
        @DisplayName("awards FAST_RESPONDER badge when responseRate >= 90")
        void awardsFastResponder() {
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(profile));
            when(badgeRepository.existsByUserIdAndBadgeType(2L, BadgeType.FAST_RESPONDER))
                    .thenReturn(false);
            when(badgeRepository.existsByUserIdAndBadgeType(eq(2L),
                    argThat(t -> t != BadgeType.FAST_RESPONDER)))
                    .thenReturn(true);
            when(badgeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            List<WorkerBadge> badges = badgeService.evaluateAndAssign(2L);

            assertThat(badges).anyMatch(b -> b.getBadgeType() == BadgeType.FAST_RESPONDER);
        }

        @Test
        @DisplayName("awards ZERO_CANCELLATIONS when 0 cancellations and >= 5 missions")
        void awardsZeroCancellations() {
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(profile));
            when(badgeRepository.existsByUserIdAndBadgeType(2L, BadgeType.ZERO_CANCELLATIONS))
                    .thenReturn(false);
            when(badgeRepository.existsByUserIdAndBadgeType(eq(2L),
                    argThat(t -> t != BadgeType.ZERO_CANCELLATIONS)))
                    .thenReturn(true);
            when(badgeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            List<WorkerBadge> badges = badgeService.evaluateAndAssign(2L);

            assertThat(badges).anyMatch(b -> b.getBadgeType() == BadgeType.ZERO_CANCELLATIONS);
        }

        @Test
        @DisplayName("getBadges() returns all badges for worker")
        void getBadges() {
            WorkerBadge badge = WorkerBadge.builder()
                    .id(1L).user(workerUser).badgeType(BadgeType.VERIFIED_CIN)
                    .label("Identité vérifiée").build();
            when(badgeRepository.findByUserId(2L)).thenReturn(List.of(badge));

            List<WorkerBadge> results = badgeService.getBadges(2L);
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getBadgeType()).isEqualTo(BadgeType.VERIFIED_CIN);
        }

        @Test
        @DisplayName("throws when worker profile not found")
        void profileNotFound() {
            when(workerProfileRepository.findByUserId(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> badgeService.evaluateAndAssign(99L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("CashGateway")
    class CashGatewayTests {

        private CashGateway cashGateway;

        @BeforeEach
        void setup() {
            cashGateway = new CashGateway();
        }

        @Test
        @DisplayName("initiate() returns asynchronous result with CASH prefix reference")
        void initiateReturnsAsynchronous() {
            GatewayResult result = cashGateway.initiate(5L, BigDecimal.valueOf(500), "order-123");

            assertThat(result.synchronous()).isFalse();
            assertThat(result.reference()).startsWith("CASH-order-123-");
            assertThat(result.redirectUrl()).isNull();
        }

        @Test
        @DisplayName("initiate() generates unique references for each call")
        void uniqueReferences() {
            GatewayResult r1 = cashGateway.initiate(5L, BigDecimal.valueOf(500), "order-123");
            GatewayResult r2 = cashGateway.initiate(5L, BigDecimal.valueOf(500), "order-123");

            assertThat(r1.reference()).isNotEqualTo(r2.reference());
        }

        @Test
        @DisplayName("verifyCallback() always returns true for cash payments")
        void verifyCallbackAlwaysTrue() {
            assertThat(cashGateway.verifyCallback(java.util.Map.of())).isTrue();
            assertThat(cashGateway.verifyCallback(java.util.Map.of("any", "param"))).isTrue();
        }

        @Test
        @DisplayName("gatewayName() returns 'CASH'")
        void gatewayName() {
            assertThat(cashGateway.gatewayName()).isEqualTo("CASH");
        }

        @Test
        @DisplayName("handles different amounts correctly")
        void handlesVariousAmounts() {
            GatewayResult r1 = cashGateway.initiate(1L, BigDecimal.valueOf(100), "ref-1");
            GatewayResult r2 = cashGateway.initiate(2L, BigDecimal.valueOf(9999.99), "ref-2");

            assertThat(r1.synchronous()).isFalse();
            assertThat(r2.synchronous()).isFalse();
        }
    }
}