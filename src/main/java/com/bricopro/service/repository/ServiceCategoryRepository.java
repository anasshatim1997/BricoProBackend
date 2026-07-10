package com.bricopro.service.repository;

import com.bricopro.service.entity.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, Long> {

    List<ServiceCategory> findByActiveTrueOrderByDisplayOrderAsc();

    Optional<ServiceCategory> findByKey(String key);

    List<ServiceCategory> findByFrNameContainingIgnoreCaseOrArNameContainingIgnoreCase(String fr, String ar);
}