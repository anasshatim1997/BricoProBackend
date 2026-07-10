package com.bricopro.verification;

import com.bricopro.upload.service.FileUploadService;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.WorkerProfile;
import com.bricopro.user.entity.WorkerProfile.VerificationStatus;
import com.bricopro.user.repository.WorkerProfileRepository;
import com.bricopro.verification.dto.CinVerificationDtos.*;
import com.bricopro.verification.ocr.TesseractCinOcrService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CinVerificationService {

    private final WorkerProfileRepository workerProfileRepository;
    private final FileUploadService fileUploadService;
    private final TesseractCinOcrService ocrService;

    @Transactional
    public CinVerificationResponse submitCin(Long userId, MultipartFile file) {
        WorkerProfile profile = workerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Worker profile not found"));

        String imageUrl = fileUploadService.uploadCinDocument(userId, file);

        String rawText;
        try {
            rawText = ocrService.extractText(file.getBytes());
        } catch (Exception e) {
            log.error("OCR extraction failed for user {}: {}", userId, e.getMessage());
            rawText = "";
        }

        String cinNumber = CinFormatValidator.extractCinNumber(rawText);
        boolean documentRecognized = CinFormatValidator.looksLikeMoroccanId(rawText);

        profile.setCinDocumentUrl(imageUrl);
        profile.setCinSubmittedAt(LocalDateTime.now());

        if (cinNumber != null && workerProfileRepository.existsByCinNumberAndIdNot(cinNumber, profile.getId())) {
            profile.setCinNumber(null);
            profile.setVerificationStatus(VerificationStatus.REJECTED);
            profile.setCinVerified(false);
            profile.setCinRejectionReason("Ce numéro de CIN est déjà utilisé par un autre compte.");
        } else if (cinNumber != null && documentRecognized) {
            profile.setCinNumber(cinNumber);
            profile.setVerificationStatus(VerificationStatus.VERIFIED);
            profile.setCinVerified(true);
            profile.setCinRejectionReason(null);
        } else if (cinNumber != null) {
            profile.setCinNumber(cinNumber);
            profile.setVerificationStatus(VerificationStatus.PENDING);
            profile.setCinVerified(false);
            profile.setCinRejectionReason(null);
        } else {
            profile.setCinNumber(null);
            profile.setVerificationStatus(VerificationStatus.REJECTED);
            profile.setCinVerified(false);
            profile.setCinRejectionReason("Document non reconnu comme une CIN valide. Réessayez avec une photo claire et bien cadrée.");
        }

        workerProfileRepository.save(profile);

        return CinVerificationResponse.builder()
                .verificationStatus(profile.getVerificationStatus())
                .cinNumber(profile.getCinNumber())
                .cinImageUrl(profile.getCinDocumentUrl())
                .message(buildMessage(profile.getVerificationStatus()))
                .build();
    }

    public WorkerProfileDetailResponse getMyProfile(Long userId) {
        WorkerProfile profile = workerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Worker profile not found"));

        User user = profile.getUser();

        List<WorkerServiceDto> services = profile.getServices() == null
                ? List.of()
                : profile.getServices().stream()
                .map(s -> WorkerServiceDto.builder()
                        .serviceType(s.getServiceType().name())
                        .hourlyRate(s.getHourlyRate())
                        .build())
                .toList();

        return WorkerProfileDetailResponse.builder()
                .id(profile.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .bio(profile.getBio())
                .city(profile.getCity())
                .interventionRadiusKm(profile.getInterventionRadiusKm())
                .latitude(profile.getLatitude())
                .longitude(profile.getLongitude())
                .bankAccount(profile.getBankAccount())
                .verified(profile.isCinVerified())
                .verificationStatus(profile.getVerificationStatus())
                .services(services)
                .rating(profile.getAverageRating())
                .completedMissions(profile.getTotalMissions())
                .reviewsCount(profile.getTotalReviews())
                .cinImageUrl(profile.getCinDocumentUrl())
                .cinRejectionReason(profile.getCinRejectionReason())
                .build();
    }

    private String buildMessage(VerificationStatus status) {
        return switch (status) {
            case VERIFIED -> "CIN vérifiée automatiquement avec succès.";
            case PENDING -> "CIN reçue, en attente de vérification manuelle.";
            case REJECTED -> "Document non reconnu, veuillez réessayer.";
            case UNSUBMITTED -> "Aucun document soumis.";
        };
    }
}