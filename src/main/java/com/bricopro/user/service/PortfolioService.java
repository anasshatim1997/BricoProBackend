package com.bricopro.user.service;

import com.bricopro.upload.service.FileUploadService;
import com.bricopro.user.entity.WorkerPortfolio;
import com.bricopro.user.entity.WorkerProfile;
import com.bricopro.user.entity.WorkerProfile.ServiceType;
import com.bricopro.user.repository.WorkerPortfolioRepository;
import com.bricopro.user.repository.WorkerProfileRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Portfolio Service", description = "Business logic for Portfolio Service")
@Service
@RequiredArgsConstructor
public class PortfolioService {

    private static final int MAX_PORTFOLIO_PHOTOS = 20;

    @Schema(description = "Portfolio Repository", example = "value")
    private final WorkerPortfolioRepository portfolioRepository;
    @Schema(description = "Worker Profile Repository", example = "value")
    private final WorkerProfileRepository   workerProfileRepository;
    @Schema(description = "Upload Service", example = "value")
    private final FileUploadService         uploadService;

    /**
     * Get Portfolio.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public List<WorkerPortfolio> getPortfolio(Long userId) {
        WorkerProfile profile = workerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Worker not found"));
        return portfolioRepository.findByWorkerProfileIdOrderByPhotoOrderAsc(profile.getId());
    }

    @Transactional
    /**
     * Add Photo.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public WorkerPortfolio addPhoto(Long userId, MultipartFile file, String caption, ServiceType serviceType) {
        WorkerProfile profile = workerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Worker profile not found"));

        if (portfolioRepository.countByWorkerProfileId(profile.getId()) >= MAX_PORTFOLIO_PHOTOS)
            throw new IllegalStateException("Maximum " + MAX_PORTFOLIO_PHOTOS + " portfolio photos allowed");

        String url = uploadService.uploadWorkerPortfolioPhoto(userId, file);
        int order = (int) portfolioRepository.countByWorkerProfileId(profile.getId());

        WorkerPortfolio photo = WorkerPortfolio.builder()
                .workerProfile(profile)
                .photoUrl(url)
                .caption(caption)
                .serviceType(serviceType)
                .photoOrder(order)
                .build();

        return portfolioRepository.save(photo);
    }

    @Transactional
    /**
     * Delete Photo.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public void deletePhoto(Long userId, Long photoId) {
        WorkerPortfolio photo = portfolioRepository.findById(photoId)
                .orElseThrow(() -> new IllegalArgumentException("Photo not found"));

        if (!photo.getWorkerProfile().getUser().getId().equals(userId))
            throw new SecurityException("Not your photo");

        portfolioRepository.delete(photo);
        uploadService.delete(photo.getPhotoUrl());
    }

    @Data
    @Schema(description = "Response body returned by: Portfolio Photo.")
    public static class PortfolioPhotoResponse {
        @Schema(description = "Unique identifier", example = "1")
        private Long id;
        @Schema(description = "Photo Url", example = "example")
        private String photoUrl;
        @Schema(description = "Caption", example = "example")
        private String caption;
        @Schema(description = "Service category: REPAIRS, PLUMBING, CLEANING, PAINTING, etc.", example = "example")
        private String serviceType;
        @Schema(description = "Photo Order", example = "0")
        private int photoOrder;
    }
}
