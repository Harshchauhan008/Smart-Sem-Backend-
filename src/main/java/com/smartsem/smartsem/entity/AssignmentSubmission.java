
package com.smartsem.smartsem.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;


@Entity
@Table(name = "assignment_submissions")
public class AssignmentSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId;


    @Column(name = "file_name", nullable = false)
    private String fileName;


    @Column(name = "file_path", nullable = false, columnDefinition = "text")
    private String filePath;


    @Column(name = "submitted_at", nullable = false)
    private OffsetDateTime submittedAt;


    @Column(columnDefinition = "text")
    private String remarks;



    // Default constructor
    public AssignmentSubmission() {
    }



    // Set submission time before saving
    @PrePersist
    public void onCreate() {

        if (submittedAt == null) {
            submittedAt = OffsetDateTime.now();
        }
    }



    // Getters and Setters

    public Long getId() {
        return id;
    }


    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }


    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }


    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }


    public OffsetDateTime getSubmittedAt() {
        return submittedAt;
    }


    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
