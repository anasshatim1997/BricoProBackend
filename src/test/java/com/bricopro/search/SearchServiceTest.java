package com.bricopro.search;

import com.bricopro.home.dto.ServiceDto;
import com.bricopro.service.ServiceCategoryService;
import com.bricopro.task.entity.Task;
import com.bricopro.task.entity.Task.TaskStatus;
import com.bricopro.task.repository.TaskRepository;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.WorkerProfile;
import com.bricopro.user.entity.WorkerProfile.ServiceType;
import com.bricopro.user.entity.WorkerService;
import com.bricopro.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchService")
class SearchServiceTest {

    @Mock ServiceCategoryService serviceCategoryService;
    @Mock UserRepository userRepository;
    @Mock TaskRepository taskRepository;

    @InjectMocks SearchService searchService;

    private User client;

    @BeforeEach
    void setup() {
        client = User.builder().id(3L).email("client@test.ma").firstName("Amina").build();

        SecurityContext context = mock(SecurityContext.class);
        var auth = new UsernamePasswordAuthenticationToken("client@test.ma", null);
        lenient().when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("returns an empty result for a null query")
    void nullQueryReturnsEmpty() {
        var result = searchService.search(null);

        assertThat(result.getServices()).isNull();
        verifyNoInteractions(serviceCategoryService, userRepository, taskRepository);
    }

    @Test
    @DisplayName("returns an empty result for a query shorter than 2 characters")
    void tooShortQueryReturnsEmpty() {
        var result = searchService.search("a");

        assertThat(result.getServices()).isNull();
        verifyNoInteractions(serviceCategoryService, userRepository, taskRepository);
    }

    @Test
    @DisplayName("searches services, workers, and the client's own tasks for a valid query")
    void searchesAllThreeSources() {
        when(userRepository.findByEmail("client@test.ma")).thenReturn(Optional.of(client));

        ServiceDto plumbing = ServiceDto.builder().key("PLUMBING").build();
        when(serviceCategoryService.searchByName("plomb")).thenReturn(List.of(plumbing));

        WorkerProfile profile = WorkerProfile.builder()
                .averageRating(BigDecimal.valueOf(4.7))
                .services(Set.of(WorkerService.builder().serviceType(ServiceType.PLUMBING).build()))
                .build();
        User worker = User.builder().id(9L).firstName("Karim").lastName("Fassi").build();
        worker.setWorkerProfile(profile);
        when(userRepository.findWorkersByNameContaining("plomb")).thenReturn(List.of(worker));

        Task task = Task.builder()
                .id(20L).title("Plomberie urgente")
                .status(TaskStatus.SEARCHING)
                .scheduledDate(LocalDate.now().plusDays(1))
                .build();
        when(taskRepository.findByTitleContainingIgnoreCaseAndClient("plomb", client))
                .thenReturn(List.of(task));

        var result = searchService.search("plomb");

        assertThat(result.getServices()).containsExactly(plumbing);
        assertThat(result.getWorkers()).hasSize(1);
        assertThat(result.getWorkers().get(0).getName()).isEqualTo("Karim Fassi");
        assertThat(result.getWorkers().get(0).getRating()).isEqualTo(4.7);
        assertThat(result.getWorkers().get(0).getService()).isEqualTo("PLUMBING");
        assertThat(result.getTasks()).hasSize(1);
        assertThat(result.getTasks().get(0).getTitle()).isEqualTo("Plomberie urgente");
    }

    @Test
    @DisplayName("trims surrounding whitespace before searching")
    void trimsQuery() {
        when(userRepository.findByEmail("client@test.ma")).thenReturn(Optional.of(client));
        when(serviceCategoryService.searchByName("plomb")).thenReturn(List.of());
        when(userRepository.findWorkersByNameContaining("plomb")).thenReturn(List.of());
        when(taskRepository.findByTitleContainingIgnoreCaseAndClient("plomb", client)).thenReturn(List.of());

        searchService.search("  plomb  ");

        verify(serviceCategoryService).searchByName("plomb");
    }

    @Test
    @DisplayName("handles a worker with no services gracefully")
    void workerWithNoServices() {
        when(userRepository.findByEmail("client@test.ma")).thenReturn(Optional.of(client));
        when(serviceCategoryService.searchByName("ka")).thenReturn(List.of());

        WorkerProfile profile = WorkerProfile.builder()
                .averageRating(BigDecimal.ZERO)
                .services(Set.of())
                .build();
        User worker = User.builder().id(9L).firstName("Karim").lastName("Fassi").build();
        worker.setWorkerProfile(profile);
        when(userRepository.findWorkersByNameContaining("ka")).thenReturn(List.of(worker));
        when(taskRepository.findByTitleContainingIgnoreCaseAndClient("ka", client)).thenReturn(List.of());

        var result = searchService.search("ka");

        assertThat(result.getWorkers().get(0).getService()).isNull();
    }
}
