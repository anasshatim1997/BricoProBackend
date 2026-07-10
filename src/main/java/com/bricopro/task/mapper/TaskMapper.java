package com.bricopro.task.mapper;

import com.bricopro.task.dto.TaskDtos.*;
import com.bricopro.task.entity.Review;
import com.bricopro.task.entity.Task;
import com.bricopro.user.dto.UserDtos.UserSummary;
import com.bricopro.user.entity.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    @Mapping(target = "client", expression = "java(toUserSummary(task.getClient()))")
    @Mapping(target = "worker", expression = "java(toUserSummary(task.getWorker()))")
    @Mapping(target = "photoUrls", expression = "java(task.getPhotos() == null ? java.util.List.of() : task.getPhotos().stream().map(p -> p.getUrl()).toList())")
    TaskResponse toResponse(Task task);

    @Mapping(target = "taskId", source = "task.id")
    @Mapping(target = "reviewer", expression = "java(toUserSummary(review.getReviewer()))")
    @Mapping(target = "reviewee", expression = "java(toUserSummary(review.getReviewee()))")
    ReviewResponse toReviewResponse(Review review);

    default UserSummary toUserSummary(User user) {
        if (user == null) return null;
        UserSummary s = new UserSummary();
        s.setId(user.getId());
        s.setFirstName(user.getFirstName());
        s.setLastName(user.getLastName());
        s.setAvatarUrl(user.getAvatarUrl());
        s.setRole(user.getRole());
        s.setStatus(user.getStatus());
        s.setVerified(user.isVerified());
        s.setOnline(user.isOnline());
        s.setCreatedAt(user.getCreatedAt());
        s.setCancellationCountThisMonth(user.getCancellationCountThisMonth());
        s.setReliabilityScore(user.getReliabilityScore());
        return s;
    }
}