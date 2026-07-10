package com.bricopro.verification;

import com.bricopro.upload.service.FileUploadService;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.WorkerProfile;
import com.bricopro.user.entity.WorkerProfile.VerificationStatus;
import com.bricopro.user.repository.WorkerProfileRepository;
import com.bricopro.verification.dto.CinVerificationDtos.CinVerificationResponse;
import com.bricopro.verification.ocr.TesseractCinOcrService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CinVerificationService")
class CinVerificationServiceTest {

    @Mock WorkerProfileRepository workerProfileRepository;
    @Mock FileUploadService fileUploadService;
    @Mock TesseractCinOcrService ocrService;

    @InjectMocks CinVerificationService cinVerificationService;

    private User worker;
    private WorkerProfile profile;
    private MultipartFile fakeFile;

    @BeforeEach
    void setup() {
        worker = User.builder().id(2L).firstName("Karim").lastName("Fassi")
                .email("karim@test.ma").build();

        profile = WorkerProfile.builder()
                .id(20L)
                .user(worker)
                .verificationStatus(VerificationStatus.UNSUBMITTED)
                .build();

        fakeFile = new MockMultipartFile("cin", "cin.jpg", "image/jpeg", "fake-bytes".getBytes());
    }

    @Nested
    @DisplayName("submitCin()")
    class SubmitCin {

        @Test
        @DisplayName("marks VERIFIED when a CIN number is extracted and the document looks Moroccan")
        void verifiedWhenNumberFoundAndDocumentRecognized() throws Exception {
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(profile));
            when(fileUploadService.uploadCinDocument(eq(2L), any())).thenReturn("http://files/cin/2/abc.jpg");
            when(ocrService.extractText(any())).thenReturn("ROYAUME DU MAROC CARTE NATIONALE AB123456");
            when(workerProfileRepository.existsByCinNumberAndIdNot("AB123456", 20L)).thenReturn(false);
            when(workerProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CinVerificationResponse response = cinVerificationService.submitCin(2L, fakeFile);

            assertThat(response.getVerificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
            assertThat(response.getCinNumber()).isEqualTo("AB123456");
            assertThat(profile.isCinVerified()).isTrue();
            assertThat(profile.getCinRejectionReason()).isNull();
        }

        @Test
        @DisplayName("marks PENDING when a CIN number is found but the document doesn't look Moroccan")
        void pendingWhenNumberFoundButDocumentNotRecognized() throws Exception {
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(profile));
            when(fileUploadService.uploadCinDocument(eq(2L), any())).thenReturn("http://files/cin/2/abc.jpg");
            when(ocrService.extractText(any())).thenReturn("AB123456");
            when(workerProfileRepository.existsByCinNumberAndIdNot("AB123456", 20L)).thenReturn(false);
            when(workerProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CinVerificationResponse response = cinVerificationService.submitCin(2L, fakeFile);

            assertThat(response.getVerificationStatus()).isEqualTo(VerificationStatus.PENDING);
            assertThat(profile.isCinVerified()).isFalse();
        }

        @Test
        @DisplayName("marks REJECTED when no CIN number can be extracted at all")
        void rejectedWhenNoNumberFound() throws Exception {
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(profile));
            when(fileUploadService.uploadCinDocument(eq(2L), any())).thenReturn("http://files/cin/2/abc.jpg");
            when(ocrService.extractText(any())).thenReturn("blurry unreadable text");
            when(workerProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CinVerificationResponse response = cinVerificationService.submitCin(2L, fakeFile);

            assertThat(response.getVerificationStatus()).isEqualTo(VerificationStatus.REJECTED);
            assertThat(profile.getCinNumber()).isNull();
            assertThat(profile.getCinRejectionReason()).contains("non reconnu");
        }

        @Test
        @DisplayName("marks REJECTED when the extracted CIN number is already used by another account")
        void rejectedWhenDuplicateCinNumber() throws Exception {
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(profile));
            when(fileUploadService.uploadCinDocument(eq(2L), any())).thenReturn("http://files/cin/2/abc.jpg");
            when(ocrService.extractText(any())).thenReturn("ROYAUME DU MAROC AB123456");
            when(workerProfileRepository.existsByCinNumberAndIdNot("AB123456", 20L)).thenReturn(true);
            when(workerProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CinVerificationResponse response = cinVerificationService.submitCin(2L, fakeFile);

            assertThat(response.getVerificationStatus()).isEqualTo(VerificationStatus.REJECTED);
            assertThat(profile.getCinNumber()).isNull();
            assertThat(profile.getCinRejectionReason()).contains("déjà utilisé");
        }

        @Test
        @DisplayName("treats OCR failure as unreadable text rather than propagating the exception")
        void ocrFailureDoesNotPropagate() throws Exception {
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(profile));
            when(fileUploadService.uploadCinDocument(eq(2L), any())).thenReturn("http://files/cin/2/abc.jpg");
            when(ocrService.extractText(any())).thenThrow(new RuntimeException("Tesseract crashed"));
            when(workerProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            CinVerificationResponse response = cinVerificationService.submitCin(2L, fakeFile);

            assertThat(response.getVerificationStatus()).isEqualTo(VerificationStatus.REJECTED);
        }

        @Test
        @DisplayName("throws when the worker profile doesn't exist")
        void throwsWhenProfileMissing() {
            when(workerProfileRepository.findByUserId(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cinVerificationService.submitCin(999L, fakeFile))
                    .isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getMyProfile()")
    class GetMyProfile {

        @Test
        @DisplayName("returns profile details including verification status")
        void returnsProfileDetails() {
            profile.setVerificationStatus(VerificationStatus.VERIFIED);
            profile.setCity("Casablanca");
            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(profile));

            var response = cinVerificationService.getMyProfile(2L);

            assertThat(response.getFirstName()).isEqualTo("Karim");
            assertThat(response.getCity()).isEqualTo("Casablanca");
            assertThat(response.getVerificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
        }

        @Test
        @DisplayName("throws when the worker profile doesn't exist")
        void throwsWhenProfileMissing() {
            when(workerProfileRepository.findByUserId(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cinVerificationService.getMyProfile(999L))
                    .isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
        }
    }
}
