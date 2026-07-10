package com.bricopro.upload.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Slf4j
public class LocalFileServeController {

    @Value("${app.upload.base-dir:/var/bricopro/uploads}")
    private String baseDir;

    private static final Map<String, MediaType> MEDIA_TYPES = Map.of(
            ".jpg",  MediaType.IMAGE_JPEG,
            ".jpeg", MediaType.IMAGE_JPEG,
            ".png",  MediaType.IMAGE_PNG,
            ".webp", MediaType.parseMediaType("image/webp"),
            ".pdf",  MediaType.APPLICATION_PDF
    );

@Operation(
    summary = "Serve Cin Document",
    description = "Serve Cin Document. Returns a paginated list of matching resources. Use the query parameters to filter and control sort order."
)
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
    @ApiResponse(responseCode = "400", description = "Validation error: check request body or query parameters", content = @Content(schema = @Schema(hidden = true))),
    @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
    @ApiResponse(responseCode = "403", description = "Forbidden: your account role cannot perform this action", content = @Content(schema = @Schema(hidden = true))),
    @ApiResponse(responseCode = "404", description = "Not found: the requested resource does not exist", content = @Content(schema = @Schema(hidden = true))),
    @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
})
@GetMapping("/files/workers/**")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Resource> serveCinDocument(
            @RequestAttribute(name = "javax.servlet.forward.request_uri", required = false) String uri,
            jakarta.servlet.http.HttpServletRequest request) {
        return serveFile(request.getRequestURI().replaceFirst("/files/", ""));
    }

@Operation(
    summary = "Serve Public File",
    description = "Serve Public File. Returns a paginated list of matching resources. Use the query parameters to filter and control sort order."
)
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Retrieved successfully"),
    @ApiResponse(responseCode = "400", description = "Validation error: check request body or query parameters", content = @Content(schema = @Schema(hidden = true))),
    @ApiResponse(responseCode = "401", description = "Unauthorised: JWT token is missing or has expired", content = @Content(schema = @Schema(hidden = true))),
    @ApiResponse(responseCode = "403", description = "Forbidden: your account role cannot perform this action", content = @Content(schema = @Schema(hidden = true))),
    @ApiResponse(responseCode = "404", description = "Not found: the requested resource does not exist", content = @Content(schema = @Schema(hidden = true))),
    @ApiResponse(responseCode = "500", description = "Internal server error: please contact support", content = @Content(schema = @Schema(hidden = true)))
})
@GetMapping("/files/**")
    public ResponseEntity<Resource> servePublicFile(
            jakarta.servlet.http.HttpServletRequest request) {
        String relativePath = request.getRequestURI().replaceFirst("/files/", "");
        return serveFile(relativePath);
    }

private ResponseEntity<Resource> serveFile(String relativePath) {
        try {
            
            Path filePath = Paths.get(baseDir).resolve(relativePath).normalize();
            if (!filePath.startsWith(Paths.get(baseDir).normalize())) {
                log.warn("Path traversal attempt blocked: {}", relativePath);
                return ResponseEntity.badRequest().build();
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String ext = getExtension(relativePath);
            MediaType mediaType = MEDIA_TYPES.getOrDefault(ext, MediaType.APPLICATION_OCTET_STREAM);

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CACHE_CONTROL,
                            mediaType == MediaType.APPLICATION_PDF
                                    ? "no-store"            
                                    : "public, max-age=86400")  
                    .body(resource);

        } catch (MalformedURLException e) {
            log.error("Could not serve file {}: {}", relativePath, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    private String getExtension(String path) {
        int dot = path.lastIndexOf('.');
        return dot >= 0 ? path.substring(dot).toLowerCase() : "";
    }
}
