package com.bricopro.search;

import com.bricopro.home.dto.SearchResultDto;
import com.bricopro.home.dto.ServiceDto;
import com.bricopro.home.dto.WorkerSummaryDto;
import com.bricopro.home.dto.TaskSummaryDto;
import com.bricopro.service.ServiceCategoryService;
import com.bricopro.task.repository.TaskRepository;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.WorkerProfile;
import com.bricopro.user.entity.WorkerService;
import com.bricopro.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {
    private final ServiceCategoryService serviceCategoryService;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    public SearchResultDto search(String query) {
        if (query == null || query.trim().length() < 2) {
            return SearchResultDto.builder().build();
        }
        String q = query.trim();

        List<ServiceDto> services = serviceCategoryService.searchByName(q);

        List<WorkerSummaryDto> workers = userRepository.findWorkersByNameContaining(q)
                .stream()
                .map(u -> {
                    WorkerProfile profile = u.getWorkerProfile();
                    Double rating = profile != null ? profile.getAverageRating().doubleValue() : null;
                    String service = null;
                    if (profile != null && profile.getServices() != null && !profile.getServices().isEmpty()) {
                        WorkerService firstService = profile.getServices().iterator().next();
                        service = firstService.getServiceType().name();
                    }
                    return WorkerSummaryDto.builder()
                            .id(u.getId())
                            .name(u.getFirstName() + " " + u.getLastName())
                            .photo(u.getAvatarUrl())
                            .rating(rating)
                            .service(service)
                            .build();
                })
                .collect(Collectors.toList());

        User client = getCurrentUser();
        List<TaskSummaryDto> tasks = taskRepository.findByTitleContainingIgnoreCaseAndClient(q, client)
                .stream()
                .map(t -> TaskSummaryDto.builder()
                        .id(t.getId())
                        .title(t.getTitle())
                        .status(t.getStatus().name())
                        .scheduledDate(t.getScheduledDate().toString())
                        .build())
                .collect(Collectors.toList());

        return SearchResultDto.builder()
                .services(services)
                .workers(workers)
                .tasks(tasks)
                .build();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}