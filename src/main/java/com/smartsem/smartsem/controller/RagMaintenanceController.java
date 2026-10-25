
package com.smartsem.smartsem.controller;

import com.smartsem.smartsem.service.AcademicDocumentService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/maintenance")
public class RagMaintenanceController {

    private final AcademicDocumentService documentService;

    public RagMaintenanceController(
            AcademicDocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/backfill-chunks")
    public Map<String, Object> backfillChunks() {
        int chunksCreated =
                documentService.backfillStudyMaterialChunks();

        return Map.of(
                "message", "Study material backfill completed",
                "chunksCreated", chunksCreated
        );
    }
}
