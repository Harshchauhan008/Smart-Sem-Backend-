
package com.smartsem.smartsem.service;

import com.smartsem.smartsem.entity.StudyMaterialChunk;
import com.smartsem.smartsem.repository.StudyMaterialChunkRepository;

import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class StudyMaterialChunkService {

    private final StudyMaterialChunkRepository chunkRepository;


    public StudyMaterialChunkService(
            StudyMaterialChunkRepository chunkRepository) {

        this.chunkRepository = chunkRepository;
    }


    // Get document chunks in their original order
    public List<StudyMaterialChunk> getChunksByDocument(
            Long documentId) {

        return chunkRepository
                .findByDocumentIdOrderByChunkIndexAsc(documentId);
    }


    // Save one text chunk
    public StudyMaterialChunk createChunk(
            StudyMaterialChunk chunk) {

        return chunkRepository.save(chunk);
    }


    // Save multiple text chunks
    public List<StudyMaterialChunk> createChunks(
            List<StudyMaterialChunk> chunks) {

        return chunkRepository.saveAll(chunks);
    }
}
