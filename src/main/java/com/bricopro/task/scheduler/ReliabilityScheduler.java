package com.bricopro.task.scheduler;

import com.bricopro.user.entity.User;
import com.bricopro.user.entity.WorkerProfile;
import com.bricopro.user.repository.UserRepository;
import com.bricopro.user.repository.WorkerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReliabilityScheduler {

    private final UserRepository userRepository;
    private final WorkerProfileRepository workerProfileRepository;

    @Transactional
    @Scheduled(cron = "0 0 0 1 * *")
    public void resetMonthlyCounters() {
        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            user.setCancellationCountThisMonth(0);
        }
        userRepository.saveAll(allUsers);

        List<WorkerProfile> profiles = workerProfileRepository.findAll();
        for (WorkerProfile profile : profiles) {
            profile.setCancellationCountThisMonth(0);
        }
        workerProfileRepository.saveAll(profiles);
    }
}