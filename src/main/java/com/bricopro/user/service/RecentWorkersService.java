package com.bricopro.user.service;

import com.bricopro.task.entity.Task;
import com.bricopro.task.repository.TaskRepository;
import com.bricopro.user.entity.User;
import com.bricopro.user.repository.UserRepository;
import com.bricopro.home.dto.RecentWorkerDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecentWorkersService {
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Transactional(readOnly = true)
    public List<RecentWorkerDto> getRecentWorkersForClient() {
        User client = getCurrentUser();
        List<Task> tasks = taskRepository.findTop10ByClientAndStatusOrderByScheduledDateDesc(
                client,
                Task.TaskStatus.COMPLETED,
                PageRequest.of(0, 10)
        );

        return tasks.stream()
                .map(Task::getWorker)
                .filter(Objects::nonNull)
                .distinct()
                .limit(5)
                .map(worker -> {
                    String lastServiceDate = tasks.stream()
                            .filter(t -> t.getWorker() != null && t.getWorker().getId().equals(worker.getId()))
                            .findFirst()
                            .map(t -> t.getScheduledDate().format(FORMATTER))
                            .orElse(null);
                    String lastServiceTitle = tasks.stream()
                            .filter(t -> t.getWorker() != null && t.getWorker().getId().equals(worker.getId()))
                            .findFirst()
                            .map(Task::getTitle)
                            .orElse(null);
                    return RecentWorkerDto.builder()
                            .workerId(worker.getId())
                            .name(worker.getFirstName() + " " + worker.getLastName())
                            .photoUrl(worker.getAvatarUrl())
                            .averageRating(worker.getWorkerProfile() != null ?
                                    worker.getWorkerProfile().getAverageRating().doubleValue() : null)
                            .lastServiceDate(lastServiceDate)
                            .lastServiceTitle(lastServiceTitle)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}