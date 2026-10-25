
package com.smartsem.smartsem.storage;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentStorageService {

    private final Path uploadDirectory =
            Paths.get("uploads").toAbsolutePath().normalize();

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png"
    );

    public DocumentStorageService() throws IOException {
        Files.createDirectories(uploadDirectory);
    }

    public String storeFile(MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please select a file.");
        }

        if (file.getSize() > 10L * 1024 * 1024) {
            throw new IllegalArgumentException(
                    "File size must not exceed 10 MB.");
        }

        String contentType = file.getContentType();

        if (contentType == null ||
                !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Only PDF, JPG and PNG files are allowed.");
        }

        String originalName = file.getOriginalFilename();
        String extension = "";

        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(
                    originalName.lastIndexOf(".")).toLowerCase();
        }

        if (!Set.of(".pdf", ".jpg", ".jpeg", ".png")
                .contains(extension)) {
            throw new IllegalArgumentException(
                    "File extension does not match an allowed type.");
        }

        String storedName = UUID.randomUUID() + extension;
        Path destination = uploadDirectory.resolve(storedName).normalize();

        if (!destination.startsWith(uploadDirectory)) {
            throw new IllegalArgumentException("Invalid file path.");
        }

        file.transferTo(destination);

        return destination.toString();
    }
}
