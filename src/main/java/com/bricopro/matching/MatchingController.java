package com.bricopro.matching;

import com.bricopro.matching.WorkerMatchingService.MatchRequest;
import com.bricopro.matching.WorkerMatchingService.MatchedWorkerDto;
import com.bricopro.task.entity.Task;
import com.bricopro.task.repository.TaskRepository;
import com.bricopro.user.entity.WorkerProfile.ServiceType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/match")
@RequiredArgsConstructor
@Tag(name = "Smart Matching")
@SecurityRequirement(name = "bearerAuth")
public class MatchingController {

    private final WorkerMatchingService matchingService;
    private final RealTimeMatchingService realTimeMatchingService;
    private final TaskRepository taskRepository;

    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "Unauthorised", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(hidden = true)))
    })
    @GetMapping("/workers")
    @Operation(summary = "Find best workers using AI scoring (distance, rating, experience, response rate)")
    public ResponseEntity<List<MatchedWorkerDto>> match(
            @Parameter(name = "lat", required = true, in = ParameterIn.QUERY) @RequestParam double lat,
            @Parameter(name = "lng", required = true, in = ParameterIn.QUERY) @RequestParam double lng,
            @Parameter(name = "radiusKm", required = true, in = ParameterIn.QUERY) @RequestParam(defaultValue = "25") double radiusKm,
            @Parameter(name = "serviceType", in = ParameterIn.QUERY) @RequestParam(required = false) ServiceType serviceType,
            @Parameter(name = "limit", required = true, in = ParameterIn.QUERY) @RequestParam(defaultValue = "10") int limit) {

        return ResponseEntity.ok(matchingService.findBestMatches(
                MatchRequest.builder()
                        .latitude(lat).longitude(lng)
                        .radiusKm(radiusKm).serviceType(serviceType)
                        .limit(Math.min(limit, 20))
                        .build()));
    }

    @GetMapping("/task/{taskId}")
    @Operation(summary = "Find workers matching a specific task (using its location, service type, schedule)")
    public ResponseEntity<List<MatchedWorkerDto>> matchForTask(@PathVariable Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        List<Long> workerIds = realTimeMatchingService.findMatchingWorkers(task);
        List<MatchedWorkerDto> dtos = workerIds.stream()
                .map(id -> MatchedWorkerDto.builder().userId(id).build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/task/{taskId}/notify")
    @Operation(summary = "Manually trigger matching and notify workers for a task")
    public ResponseEntity<Void> notifyMatchingWorkers(@PathVariable Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        List<Long> workerIds = realTimeMatchingService.findMatchingWorkers(task);
        if (!workerIds.isEmpty()) {
            realTimeMatchingService.notifyWorkers(workerIds, task);
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/task/{taskId}/auto-assign")
    @Operation(summary = "Manually auto‑assign the best matching worker to the task")
    public ResponseEntity<Void> autoAssignTask(@PathVariable Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        List<Long> workerIds = realTimeMatchingService.findMatchingWorkers(task);
        if (!workerIds.isEmpty()) {
            realTimeMatchingService.autoAssign(task, workerIds);
        }
        return ResponseEntity.ok().build();
    }
}