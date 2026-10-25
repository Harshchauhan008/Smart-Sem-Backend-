
package com.smartsem.smartsem.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "attendance_records")
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "class_date", nullable = false)
    private LocalDate classDate;

    // Identifies the specific lecture or timetable slot.
    @Column(name = "timetable_entry_id")
    private Long timetableEntryId;

    @Column(nullable = false)
    private String status;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "marked_at", nullable = false)
    private OffsetDateTime markedAt;

    public AttendanceRecord() {
    }

    @PrePersist
    public void onCreate() {
        if (markedAt == null) {
            markedAt = OffsetDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public LocalDate getClassDate() {
        return classDate;
    }

    public void setClassDate(LocalDate classDate) {
        this.classDate = classDate;
    }

    public Long getTimetableEntryId() {
        return timetableEntryId;
    }

    public void setTimetableEntryId(Long timetableEntryId) {
        this.timetableEntryId = timetableEntryId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public OffsetDateTime getMarkedAt() {
        return markedAt;
    }
}
