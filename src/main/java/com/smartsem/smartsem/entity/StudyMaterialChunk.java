
package com.smartsem.smartsem.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;


@Entity
@Table(name = "study_material_chunks")
public class StudyMaterialChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "document_id", nullable = false)
    private Long documentId;


    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;


    @Column(nullable = false, columnDefinition = "text")
    private String content;


    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;


    public StudyMaterialChunk() {
    }


    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }


    public Long getId() {
        return id;
    }


    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }


    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }


    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }


    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
