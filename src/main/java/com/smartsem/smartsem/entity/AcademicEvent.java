
package com.smartsem.smartsem.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;


@Entity
@Table(name = "academic_events")
public class AcademicEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "semester_id", nullable = false)
    private Long semesterId;


    @Column(nullable = false)
    private String title;


    @Column(columnDefinition = "text")
    private String description;


    @Column(name = "event_type", nullable = false)
    private String eventType;


    @Column(name = "starts_at")
    private OffsetDateTime startsAt;


    @Column(name = "ends_at")
    private OffsetDateTime endsAt;


    @Column(name = "source_document_id")
    private Long sourceDocumentId;


    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;


    public AcademicEvent() {
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


    public Long getSemesterId() {
        return semesterId;
    }

    public void setSemesterId(Long semesterId) {
        this.semesterId = semesterId;
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


    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }


    public OffsetDateTime getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(OffsetDateTime startsAt) {
        this.startsAt = startsAt;
    }


    public OffsetDateTime getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(OffsetDateTime endsAt) {
        this.endsAt = endsAt;
    }


    public Long getSourceDocumentId() {
        return sourceDocumentId;
    }

    public void setSourceDocumentId(Long sourceDocumentId) {
        this.sourceDocumentId = sourceDocumentId;
    }


    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
