package com.bricopro.trust;

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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkerTrustService")
class WorkerTrustServiceTest {

    @Mock WorkerRecommendationRepository recommendationRepository;
    @Mock UserRepository userRepository;

    @InjectMocks WorkerTrustService trustService;

    private User recommender;
    private User worker;

    @BeforeEach
    void setup() {
        recommender = User.builder().id(1L).firstName("Sara").build();
        worker = User.builder().id(2L).firstName("Karim").build();
    }

    @Nested
    @DisplayName("recommend()")
    class Recommend {

        @Test
        @DisplayName("saves a new recommendation")
        void savesRecommendation() {
            when(recommendationRepository.existsByRecommenderIdAndWorkerId(1L, 2L)).thenReturn(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(recommender));
            when(userRepository.findById(2L)).thenReturn(Optional.of(worker));
            when(recommendationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            WorkerRecommendation result = trustService.recommend(1L, 2L, "Great plumber");

            assertThat(result.getRecommender()).isEqualTo(recommender);
            assertThat(result.getWorker()).isEqualTo(worker);
            assertThat(result.getNote()).isEqualTo("Great plumber");
        }

        @Test
        @DisplayName("rejects recommending yourself")
        void rejectsSelfRecommendation() {
            assertThatThrownBy(() -> trustService.recommend(1L, 1L, "note"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects recommending the same worker twice")
        void rejectsDuplicateRecommendation() {
            when(recommendationRepository.existsByRecommenderIdAndWorkerId(1L, 2L)).thenReturn(true);

            assertThatThrownBy(() -> trustService.recommend(1L, 2L, "note"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("getWorkerTrustScore()")
    class GetWorkerTrustScore {

        @Test
        @DisplayName("returns HIGH trust level at 10+ recommendations")
        void highTrustLevel() {
            when(recommendationRepository.countByWorkerId(2L)).thenReturn(12L);
            when(recommendationRepository.findNetworkRecommendations(2L, 1L)).thenReturn(List.of());

            Map<String, Object> result = trustService.getWorkerTrustScore(2L, 1L);

            assertThat(result.get("trustLevel")).isEqualTo("HIGH");
            assertThat(result.get("totalRecommendations")).isEqualTo(12L);
        }

        @Test
        @DisplayName("returns MEDIUM trust level between 3 and 9 recommendations")
        void mediumTrustLevel() {
            when(recommendationRepository.countByWorkerId(2L)).thenReturn(5L);
            when(recommendationRepository.findNetworkRecommendations(2L, 1L)).thenReturn(List.of());

            Map<String, Object> result = trustService.getWorkerTrustScore(2L, 1L);

            assertThat(result.get("trustLevel")).isEqualTo("MEDIUM");
        }

        @Test
        @DisplayName("returns NEW trust level under 3 recommendations")
        void newTrustLevel() {
            when(recommendationRepository.countByWorkerId(2L)).thenReturn(1L);
            when(recommendationRepository.findNetworkRecommendations(2L, 1L)).thenReturn(List.of());

            Map<String, Object> result = trustService.getWorkerTrustScore(2L, 1L);

            assertThat(result.get("trustLevel")).isEqualTo("NEW");
            assertThat(result.get("trustedByNetwork")).isEqualTo(false);
        }

        @Test
        @DisplayName("does not query network recommendations when viewerId is null")
        void nullViewerSkipsNetworkQuery() {
            when(recommendationRepository.countByWorkerId(2L)).thenReturn(1L);

            trustService.getWorkerTrustScore(2L, null);

            verify(recommendationRepository, never()).findNetworkRecommendations(anyLong(), anyLong());
        }
    }
}
