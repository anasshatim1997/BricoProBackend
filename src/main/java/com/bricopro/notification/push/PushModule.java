package com.bricopro.notification.push;

import com.bricopro.task.entity.Task;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PushModule {

    private final PushNotificationService pushNotificationService;

    public void sendNewTaskNotification(Long workerId, Task task) {
        String title = "Nouvelle mission disponible";
        String body = task.getTitle() + " - " + task.getDescription().substring(0, Math.min(50, task.getDescription().length())) + "...";
        Map<String, String> data = new HashMap<>();
        data.put("taskId", String.valueOf(task.getId()));
        data.put("type", "NEW_TASK");
        pushNotificationService.sendToUser(workerId, title, body, data);
    }
}