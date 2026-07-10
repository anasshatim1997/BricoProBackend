package com.bricopro.booking.repository;

import com.bricopro.booking.entity.GroupBooking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupBookingRepository extends JpaRepository<GroupBooking, Long> {
    List<GroupBooking> findByStatus(GroupBooking.GroupBookingStatus status);
    List<GroupBooking> findByClientId(Long clientId);
}
