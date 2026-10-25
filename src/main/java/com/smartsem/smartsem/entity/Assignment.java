
package com.smartsem.smartsem.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;


@Entity
@Table(name = "assignments")
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "subject_id", nullable = false)
    private Long subjectId;


    @Column(nullable = false)
    private String title;


    @Column(columnDefinition = "text")
    private String description;


    @Column(name = "assigned_at")
    private OffsetDateTime assignedAt;


    @Column
    private OffsetDateTime deadline;


    @Column(nullable = false)
    private String status;


    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;


    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;



    // Default constructor
    public Assignment() {
    }



    // Set timestamps before saving
    @PrePersist
    public void onCreate() {

        OffsetDateTime now = OffsetDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }

        if (status == null || status.isBlank()) {
            status = "PENDING";
        }

        if (assignedAt == null) {
            assignedAt = OffsetDateTime.now();
        }

    }


    // Update timestamp whenever the assignment changes
    @PreUpdate
    public void onUpdate() {

        updatedAt = OffsetDateTime.now();
    }



    // Getters and Setters

    public Long getId() {
        return id;
    }


    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public OffsetDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(OffsetDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }


    public OffsetDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(OffsetDateTime deadline) {
        this.deadline = deadline;
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }


    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
