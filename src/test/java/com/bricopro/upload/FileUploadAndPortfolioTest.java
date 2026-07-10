package com.bricopro.upload;

import com.bricopro.upload.service.FileUploadService;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.User.Role;
import com.bricopro.user.entity.WorkerPortfolio;
import com.bricopro.user.entity.WorkerProfile;
import com.bricopro.user.entity.WorkerProfile.ServiceType;
import com.bricopro.user.repository.WorkerPortfolioRepository;
import com.bricopro.user.repository.WorkerProfileRepository;
import com.bricopro.user.service.PortfolioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("File Upload & Portfolio")
class FileUploadAndPortfolioTest {

    // ─── FILE UPLOAD SERVICE ──────────────────────────────────────────────────

    @Nested
    @DisplayName("FileUploadService")
    class FileUploadServiceTests {

        private FileUploadService uploadService;

        @TempDir
        Path tempDir;

        @BeforeEach
        void setup() {
            uploadService = new FileUploadService();
            ReflectionTestUtils.setField(uploadService, "baseDir", tempDir.toString());
            ReflectionTestUtils.setField(uploadService, "baseUrl", "http://localhost:8080");
            uploadService.init();
        }

        @Test
        @DisplayName("uploadAvatar saves file and returns correct URL")
        void uploadAvatarReturnsUrl() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "avatar.jpg", "image/jpeg", "fake-image-bytes".getBytes());

            String url = uploadService.uploadAvatar(1L, file);

            assertThat(url).startsWith("http://localhost:8080/files/avatars/1/");
            assertThat(url).endsWith(".jpg");
        }

        @Test
        @DisplayName("uploadCinDocument saves file and returns URL")
        void uploadCinDocumentReturnsUrl() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "cin.pdf", "application/pdf", "pdf-bytes".getBytes());

            String url = uploadService.uploadCinDocument(1L, file);

            assertThat(url).startsWith("http://localhost:8080/files/workers/1/cin/");
            assertThat(url).endsWith(".pdf");
        }

        @Test
        @DisplayName("uploadTaskPhoto saves file with taskId and userId in path")
        void uploadTaskPhotoReturnsUrl() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "photo.png", "image/png", "png-bytes".getBytes());

            String url = uploadService.uploadTaskPhoto(5L, 2L, file);

            assertThat(url).startsWith("http://localhost:8080/files/tasks/5/photos/");
        }

        @Test
        @DisplayName("uploadWorkerPortfolioPhoto saves file in portfolio folder")
        void uploadPortfolioPhotoReturnsUrl() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "work.jpg", "image/jpeg", "jpeg-bytes".getBytes());

            String url = uploadService.uploadWorkerPortfolioPhoto(2L, file);

            assertThat(url).startsWith("http://localhost:8080/files/portfolio/2/");
            assertThat(url).endsWith(".jpg");
        }

        @Test
        @DisplayName("throws when file is empty")
        void emptyFileThrows() {
            MockMultipartFile empty = new MockMultipartFile(
                    "file", "empty.jpg", "image/jpeg", new byte[0]);

            assertThatThrownBy(() -> uploadService.uploadAvatar(1L, empty))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("empty");
        }

        @Test
        @DisplayName("throws when file exceeds avatar size limit (2MB)")
        void avatarTooLargeThrows() {
            byte[] bigFile = new byte[3 * 1024 * 1024]; // 3MB
            MockMultipartFile file = new MockMultipartFile(
                    "file", "big.jpg", "image/jpeg", bigFile);

            assertThatThrownBy(() -> uploadService.uploadAvatar(1L, file))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("too large");
        }

        @Test
        @DisplayName("throws when file exceeds general size limit (5MB)")
        void fileTooLargeThrows() {
            byte[] bigFile = new byte[6 * 1024 * 1024]; // 6MB
            MockMultipartFile file = new MockMultipartFile(
                    "file", "big.pdf", "application/pdf", bigFile);

            assertThatThrownBy(() -> uploadService.uploadCinDocument(1L, file))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("too large");
        }

        @Test
        @DisplayName("throws when content type is not allowed for avatar (e.g. PDF)")
        void invalidContentTypeForAvatar() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "doc.pdf", "application/pdf", "pdf-bytes".getBytes());

            assertThatThrownBy(() -> uploadService.uploadAvatar(1L, file))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not allowed");
        }

        @Test
        @DisplayName("throws when content type is not allowed for CIN (e.g. MP4)")
        void invalidContentTypeForCin() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "video.mp4", "video/mp4", "mp4-bytes".getBytes());

            assertThatThrownBy(() -> uploadService.uploadCinDocument(1L, file))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not allowed");
        }

        @Test
        @DisplayName("WEBP is accepted as avatar content type")
        void webpAcceptedForAvatar() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "img.webp", "image/webp", "webp-bytes".getBytes());

            assertThatNoException().isThrownBy(() -> uploadService.uploadAvatar(1L, file));
        }

        @Test
        @DisplayName("delete() silently handles null URL")
        void deleteNullUrl() {
            assertThatNoException().isThrownBy(() -> uploadService.delete(null));
        }

        @Test
        @DisplayName("delete() silently handles blank URL")
        void deleteBlankUrl() {
            assertThatNoException().isThrownBy(() -> uploadService.delete("  "));
        }

        @Test
        @DisplayName("multiple uploads produce unique file names")
        void uniqueFileNames() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "avatar.jpg", "image/jpeg", "bytes".getBytes());

            String url1 = uploadService.uploadAvatar(1L, file);
            String url2 = uploadService.uploadAvatar(1L, file);

            assertThat(url1).isNotEqualTo(url2);
        }
    }

    // ─── PORTFOLIO SERVICE ────────────────────────────────────────────────────

    @Nested
    @DisplayName("PortfolioService")
    class PortfolioServiceTests {

        @Mock WorkerPortfolioRepository portfolioRepository;
        @Mock WorkerProfileRepository workerProfileRepository;
        @Mock FileUploadService uploadService;

        @InjectMocks PortfolioService portfolioService;

        private User workerUser;
        private WorkerProfile workerProfile;

        @BeforeEach
        void setup() {
            workerUser = User.builder().id(2L).firstName("Riad").lastName("Hadjami")
                    .role(Role.WORKER).build();

            workerProfile = WorkerProfile.builder()
                    .id(1L).user(workerUser).build();
        }

        @Test
        @DisplayName("getPortfolio returns all photos for worker")
        void getPortfolio() {
            WorkerPortfolio photo = WorkerPortfolio.builder()
                    .id(1L).workerProfile(workerProfile)
                    .photoUrl("http://localhost/files/portfolio/2/abc.jpg")
                    .photoOrder(0).build();

            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(workerProfile));
            when(portfolioRepository.findByWorkerProfileIdOrderByPhotoOrderAsc(1L))
                    .thenReturn(List.of(photo));

            List<WorkerPortfolio> res = portfolioService.getPortfolio(2L);
            assertThat(res).hasSize(1);
            assertThat(res.get(0).getPhotoUrl()).contains("abc.jpg");
        }

        @Test
        @DisplayName("addPhoto saves photo with correct order")
        void addPhoto() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "work.jpg", "image/jpeg", "bytes".getBytes());

            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(workerProfile));
            when(portfolioRepository.countByWorkerProfileId(1L)).thenReturn(3L);
            when(uploadService.uploadWorkerPortfolioPhoto(2L, file))
                    .thenReturn("http://localhost/files/portfolio/2/xyz.jpg");
            when(portfolioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            WorkerPortfolio result = portfolioService.addPhoto(2L, file, "My best work", ServiceType.PLUMBING);

            assertThat(result.getPhotoOrder()).isEqualTo(3);
            assertThat(result.getCaption()).isEqualTo("My best work");
            assertThat(result.getServiceType()).isEqualTo(ServiceType.PLUMBING);
        }

        @Test
        @DisplayName("addPhoto throws when portfolio exceeds 20 photos")
        void maxPhotosExceeded() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "over.jpg", "image/jpeg", "bytes".getBytes());

            when(workerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(workerProfile));
            when(portfolioRepository.countByWorkerProfileId(1L)).thenReturn(20L); // at max

            assertThatThrownBy(() -> portfolioService.addPhoto(2L, file, null, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Maximum");
        }

        @Test
        @DisplayName("deletePhoto removes photo and deletes file")
        void deletePhoto() {
            WorkerPortfolio photo = WorkerPortfolio.builder()
                    .id(1L).workerProfile(workerProfile)
                    .photoUrl("http://localhost/files/portfolio/2/to-delete.jpg").build();

            when(portfolioRepository.findById(1L)).thenReturn(Optional.of(photo));

            portfolioService.deletePhoto(2L, 1L);

            verify(portfolioRepository).delete(photo);
            verify(uploadService).delete("http://localhost/files/portfolio/2/to-delete.jpg");
        }

        @Test
        @DisplayName("deletePhoto throws SecurityException for wrong owner")
        void deletePhotoWrongOwner() {
            WorkerPortfolio photo = WorkerPortfolio.builder()
                    .id(1L).workerProfile(workerProfile)
                    .photoUrl("http://localhost/files/portfolio/2/photo.jpg").build();

            when(portfolioRepository.findById(1L)).thenReturn(Optional.of(photo));

            // userId 99 is not the owner (owner is workerUser.id = 2)
            assertThatThrownBy(() -> portfolioService.deletePhoto(99L, 1L))
                    .isInstanceOf(SecurityException.class);
        }

        @Test
        @DisplayName("deletePhoto throws when photo not found")
        void deletePhotoNotFound() {
            when(portfolioRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> portfolioService.deletePhoto(2L, 99L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
