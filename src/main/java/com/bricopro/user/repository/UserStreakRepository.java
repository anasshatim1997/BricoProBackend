package com.bricopro.user.repository;

import com.bricopro.user.entity.User;
import com.bricopro.user.entity.UserStreak;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserStreakRepository extends JpaRepository<UserStreak, Long> {
    Optional<UserStreak> findByUser(User user);
}