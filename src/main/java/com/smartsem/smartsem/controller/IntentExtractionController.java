
package com.smartsem.smartsem.controller;

import com.smartsem.smartsem.dto.IntentExtractionResponse;
import com.smartsem.smartsem.service.IntentExtractionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class IntentExtractionController {

    private final IntentExtractionService intentExtractionService;

    public IntentExtractionController(
            IntentExtractionService intentExtractionService) {
        this.intentExtractionService = intentExtractionService;
    }

    @PostMapping("/extract-intent")
    public ResponseEntity<?> extractIntent(
            @RequestBody Map<String, String> request) {

        String message = request.get("message");

        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Message is required."));
        }

        IntentExtractionResponse result =
                intentExtractionService.extractIntent(message);

        return ResponseEntity.ok(result);
    }
}
