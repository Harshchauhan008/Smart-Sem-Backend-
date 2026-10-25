
package com.smartsem.smartsem.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "academic_documents")
public class AcademicDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "semester_id", nullable = false)
    private Long semesterId;

    @Column(name = "document_type", nullable = false)
    private String documentType;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "file_path", nullable = false, columnDefinition = "text")
    private String filePath;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "processing_status", nullable = false)
    private String processingStatus = "PENDING";

    @Column(name = "extracted_text", columnDefinition = "text")
    private String extractedText;

    @Column(name = "uploaded_at", nullable = false)
    private OffsetDateTime uploadedAt;

    public AcademicDocument() {
    }

    @PrePersist
    public void onCreate() {
        if (uploadedAt == null) {
            uploadedAt = OffsetDateTime.now();
        }

        if (processingStatus == null || processingStatus.isBlank()) {
            processingStatus = "PENDING";
        }
    }

    public Long getId() {
        return id;
    }

    public Long getSemesterId() {
        return semesterId;
    }

    public void setSemesterId(Long semesterId) {
        this.semesterId = semesterId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getProcessingStatus() {
        return processingStatus;
    }

    public void setProcessingStatus(String processingStatus) {
        this.processingStatus = processingStatus;
    }

    public String getExtractedText() {
        return extractedText;
    }

    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }

    public OffsetDateTime getUploadedAt() {
        return uploadedAt;
    }
}
