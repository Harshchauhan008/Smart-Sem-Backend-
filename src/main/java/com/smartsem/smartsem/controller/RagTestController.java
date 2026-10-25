
package com.smartsem.smartsem.controller;

import com.smartsem.smartsem.entity.StudyMaterialChunk;
import com.smartsem.smartsem.repository.StudyMaterialChunkRepository;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/rag-test")
public class RagTestController {

    private final StudyMaterialChunkRepository chunkRepository;

    public RagTestController(
            StudyMaterialChunkRepository chunkRepository) {
        this.chunkRepository = chunkRepository;
    }

    @GetMapping("/search")
    public Map<String, Object> search(
            @RequestParam Long documentId,
            @RequestParam String question) {

        Set<String> stopWords = Set.of(
                "what", "when", "where", "which", "who",
                "have", "has", "does", "did", "are", "was",
                "were", "the", "and", "for", "from", "with",
                "that", "this", "your", "my", "about",
                "tell", "show", "mentioned", "listed",
                "please", "semester", "subjects"
        );

        Set<String> keywords = new LinkedHashSet<>();

        for (String word : question.toLowerCase()
                .replaceAll("[^a-z0-9 ]", " ")
                .split("\\s+")) {
            if (word.length() >= 3 && !stopWords.contains(word)) {
                keywords.add(word);
            }
            if (keywords.size() >= 8) break;
        }

        String lowerQuestion = question.toLowerCase();

        if (lowerQuestion.contains("timetable")
                || lowerQuestion.contains("subjects")) {
            keywords.add("software");
            keywords.add("cloud");
            keywords.add("programming");
        }

        Map<Long, StudyMaterialChunk> uniqueChunks =
                new LinkedHashMap<>();

        for (String keyword : keywords) {
            List<StudyMaterialChunk> matches =
                    chunkRepository.searchByDocumentAndKeyword(
                            documentId, keyword);

            for (StudyMaterialChunk chunk : matches) {
                uniqueChunks.putIfAbsent(chunk.getId(), chunk);
            }
        }

        List<Map<String, Object>> results = new ArrayList<>();

        for (StudyMaterialChunk chunk : uniqueChunks.values()) {
            results.add(Map.of(
                    "chunkId", chunk.getId(),
                    "chunkIndex", chunk.getChunkIndex(),
                    "content", chunk.getContent()
            ));
        }

        return Map.of(
                "documentId", documentId,
                "keywords", keywords,
                "matchCount", results.size(),
                "chunks", results
        );
    }
}
