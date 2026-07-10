package com.bricopro.subscription;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface RecurringTaskRepository extends JpaRepository<RecurringTask, Long> {
    Page<RecurringTask> findByClientId(Long clientId, Pageable pageable);
    List<RecurringTask> findByStatusAndNextScheduledDateLessThanEqual(
            RecurringTask.RecurringStatus status, LocalDate date);
}
