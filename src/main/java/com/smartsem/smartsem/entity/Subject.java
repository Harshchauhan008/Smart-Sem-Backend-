
package com.smartsem.smartsem.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;


@Entity
@Table(name = "subjects")
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "semester_id", nullable = false)
    private Long semesterId;


    @Column(nullable = false)
    private String name;


    @Column(name = "subject_code")
    private String subjectCode;


    @Column(name = "faculty_name")
    private String facultyName;


    @Column(name = "attendance_required", nullable = false)
    private BigDecimal attendanceRequired = new BigDecimal("75.00");


    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;



    // Default constructor
    public Subject() {
    }



    // Set creation timestamp before saving
    @PrePersist
    public void onCreate() {

        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }



    // Getters and Setters

    public Long getId() {
        return id;
    }


    public Long getSemesterId() {
        return semesterId;
    }

    public void setSemesterId(Long semesterId) {
        this.semesterId = semesterId;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }


    public String getFacultyName() {
        return facultyName;
    }

    public void setFacultyName(String facultyName) {
        this.facultyName = facultyName;
    }


    public BigDecimal getAttendanceRequired() {
        return attendanceRequired;
    }

    public void setAttendanceRequired(BigDecimal attendanceRequired) {
        this.attendanceRequired = attendanceRequired;
    }


    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

}
