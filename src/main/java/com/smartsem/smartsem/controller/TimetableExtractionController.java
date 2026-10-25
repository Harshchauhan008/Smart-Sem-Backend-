package com.smartsem.smartsem.controller;

import com.smartsem.smartsem.entity.AcademicDocument;
import com.smartsem.smartsem.repository.AcademicDocumentRepository;
import com.smartsem.smartsem.repository.SemesterRepository;
import com.smartsem.smartsem.repository.UserRepository;
import com.smartsem.smartsem.service.GeminiTimetableExtractionService;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/semesters/{semesterId}/timetable-extraction")
public class TimetableExtractionController {

    private final AcademicDocumentRepository documentRepository;
    private final SemesterRepository semesterRepository;
    private final UserRepository userRepository;
    private final GeminiTimetableExtractionService extractionService;

    public TimetableExtractionController(
            AcademicDocumentRepository documentRepository,
            SemesterRepository semesterRepository,
            UserRepository userRepository,
            GeminiTimetableExtractionService extractionService) {

        this.documentRepository = documentRepository;
        this.semesterRepository = semesterRepository;
        this.userRepository = userRepository;
        this.extractionService = extractionService;
    }

    @PostMapping("/preview")
    public Map<String, Object> preview(
            @PathVariable Long semesterId,
            Authentication authentication) {

        if (authentication == null
                || authentication.getName() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required.");
        }

        var user = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "User not found."));

        var semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Semester not found."));

        if (!semester.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You do not own this semester.");
        }

        List<AcademicDocument> documents =
                documentRepository.findBySemesterIdAndDocumentType(
                        semesterId, "TIMETABLE");

        // The preview sends the image itself to Gemini, so it does not need
        // OCR to have succeeded. It only needs the image file to still exist.
        AcademicDocument latest = documents.stream()
                .filter(d -> d.getFilePath() != null
                        && !d.getFilePath().isBlank())
                .filter(d -> Files.exists(Path.of(d.getFilePath())))
                .max(Comparator.comparing(AcademicDocument::getId))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No timetable image was found. Please upload it again."));

        return extractionService.extractTimetable(
                semesterId,
                latest.getFilePath());
    }
}