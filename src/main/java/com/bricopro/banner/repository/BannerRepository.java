package com.bricopro.banner.repository;

import com.bricopro.banner.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface BannerRepository extends JpaRepository<Banner, Long> {
    @Query("SELECT b FROM Banner b WHERE b.active = true AND b.startDate <= :now AND b.endDate >= :now ORDER BY b.displayOrder ASC")
    List<Banner> findActiveBanners(@Param("now") LocalDateTime now);
}