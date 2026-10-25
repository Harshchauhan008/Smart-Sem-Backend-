
package com.smartsem.smartsem.repository;

import com.smartsem.smartsem.entity.StudyMaterialChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudyMaterialChunkRepository
        extends JpaRepository<StudyMaterialChunk, Long> {

    List<StudyMaterialChunk> findByDocumentIdOrderByChunkIndexAsc(
            Long documentId);

    @Query("""
        SELECT c FROM StudyMaterialChunk c
        WHERE c.documentId = :documentId
          AND LOWER(c.content) LIKE LOWER(
              CONCAT('%', :keyword, '%')
          )
        ORDER BY c.chunkIndex ASC
        """)
    List<StudyMaterialChunk> searchByDocumentAndKeyword(
            @Param("documentId") Long documentId,
            @Param("keyword") String keyword);
}
