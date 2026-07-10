package com.bricopro.user;

import com.bricopro.user.entity.User;
import com.bricopro.user.entity.UserStreak;
import com.bricopro.user.repository.UserRepository;
import com.bricopro.user.repository.UserStreakRepository;
import com.bricopro.user.service.StreakService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StreakService")
class StreakServiceTest {

    @Mock UserStreakRepository streakRepository;
    @Mock UserRepository userRepository;

    @InjectMocks StreakService streakService;

    private User user;

    @BeforeEach
    void setup() {
        user = User.builder().id(3L).email("amina@test.ma").build();

        SecurityContext context = mock(SecurityContext.class);
        var auth = new UsernamePasswordAuthenticationToken("amina@test.ma", null);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
        when(userRepository.findByEmail("amina@test.ma")).thenReturn(Optional.of(user));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("getCurrentStreak()")
    class GetCurrentStreak {

        @Test
        @DisplayName("returns the stored streak count")
        void returnsStoredStreak() {
            UserStreak streak = UserStreak.builder().user(user).currentStreak(5).build();
            when(streakRepository.findByUser(user)).thenReturn(Optional.of(streak));

            assertThat(streakService.getCurrentStreak()).isEqualTo(5);
        }

        @Test
        @DisplayName("returns 0 when the user has never checked in")
        void returnsZeroWhenNoStreakYet() {
            when(streakRepository.findByUser(user)).thenReturn(Optional.empty());

            assertThat(streakService.getCurrentStreak()).isZero();
        }
    }

    @Nested
    @DisplayName("checkIn()")
    class CheckIn {

        @Test
        @DisplayName("starts a streak of 1 on first-ever check-in")
        void firstEverCheckIn() {
            when(streakRepository.findByUser(user)).thenReturn(Optional.empty());
            when(streakRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            int result = streakService.checkIn();

            assertThat(result).isEqualTo(1);
            verify(streakRepository).save(argThat(s ->
                    s.getCurrentStreak() == 1 && s.getMaxStreak() == 1
                            && s.getLastActiveDate().equals(LocalDate.now())));
        }

        @Test
        @DisplayName("increments the streak on a consecutive day")
        void consecutiveDayIncrements() {
            UserStreak streak = UserStreak.builder()
                    .user(user).currentStreak(4).maxStreak(4)
                    .lastActiveDate(LocalDate.now().minusDays(1))
                    .build();
            when(streakRepository.findByUser(user)).thenReturn(Optional.of(streak));
            when(streakRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            int result = streakService.checkIn();

            assertThat(result).isEqualTo(5);
            assertThat(streak.getMaxStreak()).isEqualTo(5);
            assertThat(streak.getLastActiveDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("resets to 1 when the previous check-in was more than a day ago")
        void brokenStreakResets() {
            UserStreak streak = UserStreak.builder()
                    .user(user).currentStreak(10).maxStreak(10)
                    .lastActiveDate(LocalDate.now().minusDays(3))
                    .build();
            when(streakRepository.findByUser(user)).thenReturn(Optional.of(streak));
            when(streakRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            int result = streakService.checkIn();

            assertThat(result).isEqualTo(1);
            assertThat(streak.getMaxStreak()).isEqualTo(10);
        }

        @Test
        @DisplayName("does not double-count a second check-in on the same day")
        void sameDayCheckInIsNoOp() {
            UserStreak streak = UserStreak.builder()
                    .user(user).currentStreak(7).maxStreak(7)
                    .lastActiveDate(LocalDate.now())
                    .build();
            when(streakRepository.findByUser(user)).thenReturn(Optional.of(streak));

            int result = streakService.checkIn();

            assertThat(result).isEqualTo(7);
            verify(streakRepository, never()).save(any());
        }

        @Test
        @DisplayName("preserves maxStreak when the current streak breaks below the previous max")
        void maxStreakPreservedAfterBreak() {
            UserStreak streak = UserStreak.builder()
                    .user(user).currentStreak(15).maxStreak(15)
                    .lastActiveDate(LocalDate.now().minusDays(5))
                    .build();
            when(streakRepository.findByUser(user)).thenReturn(Optional.of(streak));
            when(streakRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            streakService.checkIn();

            assertThat(streak.getCurrentStreak()).isEqualTo(1);
            assertThat(streak.getMaxStreak()).isEqualTo(15);
        }
    }
}
