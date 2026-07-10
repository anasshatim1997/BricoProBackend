package com.bricopro.task.controller;

import com.bricopro.task.entity.Task;
import com.bricopro.task.repository.TaskRepository;
import com.bricopro.task.service.CancellationService;
import com.bricopro.task.service.CancellationService.CancellationResult;
import com.bricopro.user.entity.User;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Tag(name = "Tasks")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class CancellationController {

    private final CancellationService cancellationService;
    private final TaskRepository taskRepository;

    @PostMapping("/{taskId}/cancel")
    @PreAuthorize("hasAnyRole('CLIENT', 'WORKER', 'ADMIN')")
    public ResponseEntity<CancellationResult> cancelTask(
            @PathVariable Long taskId,
            @RequestBody CancelRequest request,
            @AuthenticationPrincipal User user) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        Long actorId = user.getId();
        boolean isClient = task.getClient().getId().equals(actorId);
        boolean isWorker = task.getWorker() != null && task.getWorker().getId().equals(actorId);
        boolean isAdmin = user.getRole() == User.Role.ADMIN;

        if (!isClient && !isWorker && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a participant");
        }

        if (task.getStatus() == Task.TaskStatus.COMPLETED ||
                task.getStatus() == Task.TaskStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task already completed or cancelled");
        }

        CancellationResult result = cancellationService.cancel(task, actorId, request.getReason());
        return ResponseEntity.ok(result);
    }

    @Data
    public static class CancelRequest {
        @NotBlank
        private String reason;
    }
}
