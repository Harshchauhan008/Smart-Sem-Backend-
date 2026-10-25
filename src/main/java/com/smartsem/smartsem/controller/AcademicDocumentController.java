
package com.smartsem.smartsem.controller;

import com.smartsem.smartsem.entity.AcademicDocument;
import com.smartsem.smartsem.entity.User;
import com.smartsem.smartsem.repository.SemesterRepository;
import com.smartsem.smartsem.repository.UserRepository;
import com.smartsem.smartsem.service.AcademicDocumentService;
import com.smartsem.smartsem.service.DocumentTextExtractionService;
import com.smartsem.smartsem.storage.DocumentStorageService;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/semesters/{semesterId}/documents")
public class AcademicDocumentController {

    private final AcademicDocumentService documentService;
    private final SemesterRepository semesterRepository;
    private final UserRepository userRepository;
    private final DocumentStorageService storageService;
    private final DocumentTextExtractionService textExtractionService;

    public AcademicDocumentController(
            AcademicDocumentService documentService,
            SemesterRepository semesterRepository,
            UserRepository userRepository,
            DocumentStorageService storageService,
            DocumentTextExtractionService textExtractionService) {

        this.documentService = documentService;
        this.semesterRepository = semesterRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.textExtractionService = textExtractionService;
    }

    private boolean ownsSemester(
            Long semesterId,
            Authentication authentication) {

        if (authentication == null) {
            return false;
        }

        User user = userRepository
                .findByEmail(authentication.getName())
                .orElse(null);

        if (user == null) {
            return false;
        }

        return semesterRepository.findById(semesterId)
                .map(semester ->
                        semester.getUserId().equals(user.getId()))
                .orElse(false);
    }

    // GET: Retrieve documents for a semester
    @GetMapping
    public ResponseEntity<?> getDocuments(
            @PathVariable Long semesterId,
            @RequestParam(required = false) String type,
            Authentication authentication) {

        if (!ownsSemester(semesterId, authentication)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Semester not found"));
        }

        List<AcademicDocument> documents;

        if (type != null && !type.isBlank()) {
            documents = documentService.getDocumentsByType(
                    semesterId,
                    type.trim().toUpperCase());
        } else {
            documents = documentService
                    .getDocumentsBySemester(semesterId);
        }

        return ResponseEntity.ok(documents);
    }

    // POST: Save document metadata (existing endpoint)
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createDocument(
            @PathVariable Long semesterId,
            @RequestBody AcademicDocument document,
            Authentication authentication) {

        if (!ownsSemester(semesterId, authentication)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Semester not found"));
        }

        if (document.getOriginalFileName() == null
                || document.getOriginalFileName().isBlank()
                || document.getFilePath() == null
                || document.getFilePath().isBlank()
                || document.getDocumentType() == null
                || document.getDocumentType().isBlank()) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "File name, file path and document type are required"));
        }

        String type = document.getDocumentType()
                .trim().toUpperCase();

        if (!List.of(
                "TIMETABLE", "SYLLABUS", "DATESHEET", "OTHER"
        ).contains(type)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid document type"));
        }

        
        document.setSemesterId(semesterId);
        document.setDocumentType(type);
        document.setProcessingStatus("PENDING");

        AcademicDocument savedDocument =
                documentService.createDocument(document);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savedDocument);
    }

    // POST: Upload a PDF, JPG or PNG and extract its text
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadDocument(
            @PathVariable Long semesterId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") String documentType,
            Authentication authentication) {

        if (!ownsSemester(semesterId, authentication)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Semester not found"));
        }

        if (documentType == null || !List.of(
                "TIMETABLE", "SYLLABUS", "DATESHEET", "OTHER"
        ).contains(documentType.trim().toUpperCase())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Invalid document type"));
        }

        String filePath;

        try {
            filePath = storageService.storeFile(file);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Could not store uploaded file"));
        }

        AcademicDocument document = new AcademicDocument();
        document.setSemesterId(semesterId);
        document.setDocumentType(documentType.trim().toUpperCase());
        document.setOriginalFileName(file.getOriginalFilename());
        document.setFilePath(filePath);
        document.setMimeType(file.getContentType());

        try {
            String extractedText =
                    textExtractionService.extractText(Path.of(filePath));

            document.setExtractedText(extractedText);
            document.setProcessingStatus("PROCESSED");

        } catch (Exception e) {
            document.setExtractedText(null);
            document.setProcessingStatus("FAILED");
        }

        try {
            AcademicDocument savedDocument =
                    documentService.createDocument(document);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(savedDocument);

        } catch (Exception e) {
            try {
                Files.deleteIfExists(Path.of(filePath));
            } catch (Exception ignored) {
                // Do not hide the original database error.
            }

            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Could not save document metadata"));
        }
    }
}
