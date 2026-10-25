
package com.smartsem.smartsem.controller;

import com.smartsem.smartsem.entity.AcademicDocument;
import com.smartsem.smartsem.entity.User;
import com.smartsem.smartsem.entity.StudyMaterialChunk;

import com.smartsem.smartsem.repository.AcademicDocumentRepository;
import com.smartsem.smartsem.repository.SemesterRepository;
import com.smartsem.smartsem.repository.UserRepository;

import com.smartsem.smartsem.service.StudyMaterialChunkService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/documents/{documentId}/chunks")
public class StudyMaterialChunkController {

    private final StudyMaterialChunkService chunkService;
    private final AcademicDocumentRepository documentRepository;
    private final SemesterRepository semesterRepository;
    private final UserRepository userRepository;


    public StudyMaterialChunkController(
            StudyMaterialChunkService chunkService,
            AcademicDocumentRepository documentRepository,
            SemesterRepository semesterRepository,
            UserRepository userRepository) {

        this.chunkService = chunkService;
        this.documentRepository = documentRepository;
        this.semesterRepository = semesterRepository;
        this.userRepository = userRepository;
    }


    // Verify that the document belongs to the logged-in user
    private boolean ownsDocument(
            Long documentId,
            Authentication authentication) {

        User user = userRepository
                .findByEmail(authentication.getName())
                .orElse(null);

        if (user == null) {
            return false;
        }

        AcademicDocument document = documentRepository
                .findById(documentId)
                .orElse(null);

        if (document == null) {
            return false;
        }

        return semesterRepository
                .findById(document.getSemesterId())
                .map(semester ->
                        semester.getUserId().equals(user.getId()))
                .orElse(false);
    }


    // GET: Retrieve all chunks for a document
    @GetMapping
    public ResponseEntity<?> getChunks(
            @PathVariable Long documentId,
            Authentication authentication) {

        if (!ownsDocument(documentId, authentication)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message",
                            "Document not found"
                    ));
        }

        List<StudyMaterialChunk> chunks =
                chunkService.getChunksByDocument(documentId);

        return ResponseEntity.ok(chunks);
    }
}
