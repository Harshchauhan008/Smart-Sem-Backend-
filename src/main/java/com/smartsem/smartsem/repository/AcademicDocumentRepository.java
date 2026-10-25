
package com.smartsem.smartsem.repository;

import com.smartsem.smartsem.entity.AcademicDocument;
import com.smartsem.smartsem.entity.StudyMaterialChunk;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface AcademicDocumentRepository
        extends JpaRepository<AcademicDocument, Long> {

    // Get all documents for a semester
    List<AcademicDocument> findBySemesterId(Long semesterId);


    // Get documents by semester and document type
    List<AcademicDocument> findBySemesterIdAndDocumentType(
            Long semesterId,
            String documentType
    );

    
@org.springframework.data.jpa.repository.Query("""
    SELECT c FROM StudyMaterialChunk c
    WHERE c.documentId = :documentId
      AND LOWER(c.content) LIKE LOWER(
          CONCAT('%', :keyword, '%')
      )
    ORDER BY c.chunkIndex ASC
    """)
List<StudyMaterialChunk> searchByDocumentAndKeyword(
        @org.springframework.data.repository.query.Param("documentId")
        Long documentId,
        @org.springframework.data.repository.query.Param("keyword")
        String keyword
);

}
