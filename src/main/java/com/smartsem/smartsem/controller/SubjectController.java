
package com.smartsem.smartsem.controller;

import com.smartsem.smartsem.entity.Semester;
import com.smartsem.smartsem.entity.Subject;
import com.smartsem.smartsem.entity.User;

import com.smartsem.smartsem.repository.SemesterRepository;
import com.smartsem.smartsem.repository.SubjectRepository;
import com.smartsem.smartsem.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/semesters/{semesterId}/subjects")
public class SubjectController {

    private final SemesterRepository semesterRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;

    public SubjectController(
            SemesterRepository semesterRepository,
            SubjectRepository subjectRepository,
            UserRepository userRepository) {

        this.semesterRepository = semesterRepository;
        this.subjectRepository = subjectRepository;
        this.userRepository = userRepository;
    }

    // Get all subjects for an owned semester
    @GetMapping
    public List<Subject> getSubjects(
            @PathVariable Long semesterId,
            Authentication authentication) {

        User user = getLoggedInUser(authentication);
        getOwnedSemester(semesterId, user.getId());

        return subjectRepository.findBySemesterId(semesterId);
    }

    // Create a subject
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Subject createSubject(
            @PathVariable Long semesterId,
            @RequestBody Subject subject,
            Authentication authentication) {

        User user = getLoggedInUser(authentication);
        getOwnedSemester(semesterId, user.getId());

        if (subject.getName() == null
                || subject.getName().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Subject name is required"
            );
        }

        String name = subject.getName().trim();

        subjectRepository
                .findBySemesterIdAndNameIgnoreCase(
                        semesterId, name)
                .ifPresent(existing -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "A subject with this name already exists in this semester"
                    );
                });

        subject.setName(name);
        subject.setSemesterId(semesterId);

        return subjectRepository.save(subject);
    }

    // Update a subject within its semester
    @PutMapping("/{subjectId}")
    public Subject updateSubject(
            @PathVariable Long semesterId,
            @PathVariable Long subjectId,
            @RequestBody Subject request,
            Authentication authentication) {

        User user = getLoggedInUser(authentication);
        getOwnedSemester(semesterId, user.getId());

        Subject subject = subjectRepository
                .findByIdAndSemesterId(subjectId, semesterId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Subject not found in this semester"
                        )
                );

        if (request.getName() == null
                || request.getName().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Subject name is required"
            );
        }

        String name = request.getName().trim();

        subjectRepository
                .findBySemesterIdAndNameIgnoreCase(
                        semesterId, name)
                .filter(existing ->
                        !existing.getId().equals(subjectId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Another subject with this name already exists"
                    );
                });

        subject.setName(name);
        subject.setSubjectCode(request.getSubjectCode());
        subject.setFacultyName(request.getFacultyName());
        subject.setAttendanceRequired(
                request.getAttendanceRequired());

        // Keep the existing subject ID and semester ID
        return subjectRepository.save(subject);
    }

    // Delete a subject within its semester
    @DeleteMapping("/{subjectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSubject(
            @PathVariable Long semesterId,
            @PathVariable Long subjectId,
            Authentication authentication) {

        User user = getLoggedInUser(authentication);
        getOwnedSemester(semesterId, user.getId());

        Subject subject = subjectRepository
                .findByIdAndSemesterId(subjectId, semesterId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Subject not found in this semester"
                        )
                );

        subjectRepository.delete(subject);
    }

    private User getLoggedInUser(
            Authentication authentication) {

        return userRepository.findByEmail(
                authentication.getName()
        ).orElseThrow(() ->
                new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                )
        );
    }

    private Semester getOwnedSemester(
            Long semesterId, Long userId) {

        return semesterRepository.findById(semesterId)
                .filter(semester ->
                        semester.getUserId().equals(userId))
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Semester not found"
                        )
                );
    }
}
