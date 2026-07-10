package com.bricopro.user;

import com.bricopro.user.entity.ClientFavorite;
import com.bricopro.user.entity.User;
import com.bricopro.user.repository.ClientFavoriteRepository;
import com.bricopro.user.repository.UserRepository;
import com.bricopro.user.service.FavoritesService;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FavoritesService")
class FavoritesServiceTest {

    @Mock ClientFavoriteRepository favoriteRepository;
    @Mock UserRepository userRepository;

    @InjectMocks FavoritesService favoritesService;

    private User client;
    private User worker;

    @BeforeEach
    void setup() {
        client = User.builder().id(3L).email("amina@test.ma").build();
        worker = User.builder().id(9L).email("karim@test.ma").build();

        SecurityContext context = mock(SecurityContext.class);
        var auth = new UsernamePasswordAuthenticationToken("amina@test.ma", null);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("getFavoriteWorkers()")
    class GetFavoriteWorkers {

        @Test
        @DisplayName("returns the client's favorited workers")
        void returnsFavorites() {
            when(userRepository.findByEmail("amina@test.ma")).thenReturn(Optional.of(client));
            ClientFavorite fav = ClientFavorite.builder().client(client).worker(worker).build();
            when(favoriteRepository.findByClient(client)).thenReturn(List.of(fav));

            List<User> result = favoritesService.getFavoriteWorkers();

            assertThat(result).containsExactly(worker);
        }

        @Test
        @DisplayName("returns an empty list when no favorites exist")
        void returnsEmptyWhenNone() {
            when(userRepository.findByEmail("amina@test.ma")).thenReturn(Optional.of(client));
            when(favoriteRepository.findByClient(client)).thenReturn(List.of());

            assertThat(favoritesService.getFavoriteWorkers()).isEmpty();
        }
    }

    @Nested
    @DisplayName("addFavorite()")
    class AddFavorite {

        @Test
        @DisplayName("saves a new favorite")
        void savesNewFavorite() {
            when(userRepository.findByEmail("amina@test.ma")).thenReturn(Optional.of(client));
            when(userRepository.findById(9L)).thenReturn(Optional.of(worker));
            when(favoriteRepository.existsByClientAndWorker(client, worker)).thenReturn(false);

            favoritesService.addFavorite(9L);

            verify(favoriteRepository).save(argThat(fav ->
                    fav.getClient().equals(client) && fav.getWorker().equals(worker)));
        }

        @Test
        @DisplayName("silently does nothing when already a favorite, rather than throwing")
        void silentNoOpOnDuplicate() {
            when(userRepository.findByEmail("amina@test.ma")).thenReturn(Optional.of(client));
            when(userRepository.findById(9L)).thenReturn(Optional.of(worker));
            when(favoriteRepository.existsByClientAndWorker(client, worker)).thenReturn(true);

            favoritesService.addFavorite(9L);

            verify(favoriteRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when the worker doesn't exist")
        void throwsWhenWorkerMissing() {
            when(userRepository.findByEmail("amina@test.ma")).thenReturn(Optional.of(client));
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> favoritesService.addFavorite(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Worker not found");
        }
    }

    @Nested
    @DisplayName("removeFavorite()")
    class RemoveFavorite {

        @Test
        @DisplayName("deletes the favorite relationship")
        void deletesFavorite() {
            when(userRepository.findByEmail("amina@test.ma")).thenReturn(Optional.of(client));
            when(userRepository.findById(9L)).thenReturn(Optional.of(worker));

            favoritesService.removeFavorite(9L);

            verify(favoriteRepository).deleteByClientAndWorker(client, worker);
        }

        @Test
        @DisplayName("throws when the worker doesn't exist")
        void throwsWhenWorkerMissing() {
            when(userRepository.findByEmail("amina@test.ma")).thenReturn(Optional.of(client));
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> favoritesService.removeFavorite(999L))
                    .isInstanceOf(RuntimeException.class);
        }
    }
}
