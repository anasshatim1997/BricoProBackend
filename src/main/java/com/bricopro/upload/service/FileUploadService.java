package com.bricopro.upload.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "File Upload Service", description = "Business logic for File Upload Service")
@Service
@Slf4j
public class FileUploadService {

    @Value("${app.upload.base-dir:/var/bricopro/uploads}")
    private String baseDir;

    @Value("${app.upload.base-url:http://localhost:8080}")
    private String baseUrl;

    private static final long   MAX_FILE_SIZE        = 5 * 1024 * 1024; 
    private static final long   MAX_AVATAR_SIZE      = 2 * 1024 * 1024; 
    private static final List<String> ALLOWED_IMAGES = List.of("image/jpeg", "image/png", "image/webp");
    private static final List<String> ALLOWED_DOCS   = List.of("image/jpeg", "image/png", "application/pdf");

@PostConstruct
    /**
     * Init.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public void init() {
        List.of("avatars", "workers", "tasks", "portfolio").forEach(sub -> {
            try {
                Files.createDirectories(Paths.get(baseDir, sub));
                log.info("Upload directory ready: {}/{}", baseDir, sub);
            } catch (IOException e) {
                throw new RuntimeException("Cannot create upload directory: " + baseDir + "/" + sub, e);
            }
        });
    }

/**
 * Upload Avatar.
 * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
 */
public String uploadAvatar(Long userId, MultipartFile file) {
        validate(file, ALLOWED_IMAGES, MAX_AVATAR_SIZE);
        String relativePath = "avatars/" + userId + "/" + uuid() + ext(file);
        save(file, relativePath);
        return url(relativePath);
    }

    /**
     * Upload Cin Document.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public String uploadCinDocument(Long userId, MultipartFile file) {
        validate(file, ALLOWED_DOCS, MAX_FILE_SIZE);
        
        String relativePath = "workers/" + userId + "/cin/" + uuid() + ext(file);
        save(file, relativePath);
        return url(relativePath);
    }

    /**
     * Upload Task Photo.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public String uploadTaskPhoto(Long taskId, Long userId, MultipartFile file) {
        validate(file, ALLOWED_IMAGES, MAX_FILE_SIZE);
        String relativePath = "tasks/" + taskId + "/photos/" + userId + "_" + uuid() + ext(file);
        save(file, relativePath);
        return url(relativePath);
    }

    /**
     * Upload Worker Portfolio Photo.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public String uploadWorkerPortfolioPhoto(Long workerId, MultipartFile file) {
        validate(file, ALLOWED_IMAGES, MAX_FILE_SIZE);
        String relativePath = "portfolio/" + workerId + "/" + uuid() + ext(file);
        save(file, relativePath);
        return url(relativePath);
    }

    /**
     * Delete.
     * <p>Business-layer implementation. All domain rules and validations for this operation are enforced here.</p>
     */
    public void delete(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return;
        try {
            
            String relative = fileUrl.replace(baseUrl + "/files/", "");
            Path target = Paths.get(baseDir, relative).normalize();

if (!target.startsWith(Paths.get(baseDir).normalize())) {
                log.warn("Blocked attempt to delete outside upload directory: {}", target);
                return;
            }
            Files.deleteIfExists(target);
            log.info("Deleted local file: {}", target);
        } catch (IOException e) {
            log.error("Failed to delete file {}: {}", fileUrl, e.getMessage());
        }
    }

private void save(MultipartFile file, String relativePath) {
        try {
            Path target = Paths.get(baseDir, relativePath);
            Files.createDirectories(target.getParent());
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            log.info("Saved file: {}", target);
        } catch (IOException e) {
            throw new RuntimeException("File save failed: " + e.getMessage(), e);
        }
    }

    private void validate(MultipartFile file, List<String> allowed, long maxSize) {
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("File is empty");
        if (file.getSize() > maxSize)
            throw new IllegalArgumentException(
                    "File too large. Max: " + (maxSize / 1024 / 1024) + " MB");
        if (!allowed.contains(file.getContentType()))
            throw new IllegalArgumentException("File type not allowed: " + file.getContentType());
    }

    private String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String ext(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name == null || !name.contains(".")) return "";
        return name.substring(name.lastIndexOf(".")).toLowerCase();
    }

    private String url(String relativePath) {
        return baseUrl + "/files/" + relativePath;
    }
}
