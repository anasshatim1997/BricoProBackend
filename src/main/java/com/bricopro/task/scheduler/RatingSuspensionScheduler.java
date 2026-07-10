package com.bricopro.task.scheduler;

import com.bricopro.task.service.RatingSuspensionService;
import com.bricopro.user.entity.User.Status;
import com.bricopro.user.entity.WorkerProfile;
import com.bricopro.user.repository.WorkerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RatingSuspensionScheduler {

    private final WorkerProfileRepository workerProfileRepository;
    private final RatingSuspensionService ratingSuspensionService;

    @Scheduled(cron = "0 0 3 * * *")
    public void sweepActiveWorkers() {
        int page = 0;
        Page<WorkerProfile> batch;
        do {
            batch = workerProfileRepository.findByUserStatus(Status.ACTIVE, PageRequest.of(page, 200));
            for (WorkerProfile profile : batch.getContent()) {
                ratingSuspensionService.evaluateWorkerRating(profile.getUser().getId());
            }
            page++;
        } while (batch.hasNext());
    }
}
