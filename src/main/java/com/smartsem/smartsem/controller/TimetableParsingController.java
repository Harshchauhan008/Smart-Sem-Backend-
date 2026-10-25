
package com.smartsem.smartsem.controller;

import com.smartsem.smartsem.entity.AcademicDocument;
import com.smartsem.smartsem.repository.AcademicDocumentRepository;
import com.smartsem.smartsem.service.TimetableParsingService;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/semesters/{semesterId}/timetable-parser")
public class TimetableParsingController {

    private final AcademicDocumentRepository documentRepository;
    private final TimetableParsingService parsingService;

    public TimetableParsingController(
            AcademicDocumentRepository documentRepository,
            TimetableParsingService parsingService) {
        this.documentRepository = documentRepository;
        this.parsingService = parsingService;
    }

    @PostMapping("/preview")
    public List<String> preview(
            @PathVariable Long semesterId,
            Authentication authentication) {

        List<AcademicDocument> documents =
                documentRepository.findBySemesterIdAndDocumentType(
                        semesterId, "TIMETABLE");

        AcademicDocument document = documents.stream()
                .filter(d -> d.getExtractedText() != null
                        && !d.getExtractedText().isBlank())
                .reduce((first, second) -> second)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No timetable with extracted text was found"));

        return parsingService.findRecognizedSchedulePatterns(
                document.getExtractedText());
    }
}
