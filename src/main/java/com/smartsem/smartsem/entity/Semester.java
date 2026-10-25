
package com.smartsem.smartsem.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;


@Entity
@Table(name = "semesters")
public class Semester {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "user_id", nullable = false)
    private Long userId;


    @Column(nullable = false)
    private String name;


    @Column(name = "academic_year")
    private String academicYear;


    @Column(name = "semester_number")
    private Integer semesterNumber;


    @Column(name = "start_date")
    private LocalDate startDate;


    @Column(name = "end_date")
    private LocalDate endDate;


    @Column(name = "is_active", nullable = false)
    private boolean active = false;


    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;



    // Default constructor
    public Semester() {
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


    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }


    public Integer getSemesterNumber() {
        return semesterNumber;
    }

    public void setSemesterNumber(Integer semesterNumber) {
        this.semesterNumber = semesterNumber;
    }


    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }


    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }


    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }


    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

}
