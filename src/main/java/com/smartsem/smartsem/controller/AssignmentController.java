
package com.smartsem.smartsem.controller;

import com.smartsem.smartsem.entity.Assignment;
import com.smartsem.smartsem.entity.Semester;
import com.smartsem.smartsem.entity.Subject;
import com.smartsem.smartsem.entity.User;

import com.smartsem.smartsem.repository.AssignmentRepository;
import com.smartsem.smartsem.repository.SemesterRepository;
import com.smartsem.smartsem.repository.SubjectRepository;
import com.smartsem.smartsem.repository.UserRepository;
import com.smartsem.smartsem.service.NotificationService;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/subjects/{subjectId}/assignments")
public class AssignmentController {

    private final AssignmentRepository assignmentRepository;
    private final SubjectRepository subjectRepository;
    private final SemesterRepository semesterRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public AssignmentController(
            AssignmentRepository assignmentRepository,
            SubjectRepository subjectRepository,
            SemesterRepository semesterRepository,
            UserRepository userRepository,
            NotificationService notificationService) {

        this.assignmentRepository = assignmentRepository;
        this.subjectRepository = subjectRepository;
        this.semesterRepository = semesterRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<Assignment> getAssignments(
            @PathVariable Long subjectId,
            @RequestParam(required = false) String status,
            Authentication authentication) {

        User user = getLoggedInUser(authentication);
        getOwnedSubject(subjectId, user.getId());

        if (status != null && !status.isBlank()) {
            return assignmentRepository.findBySubjectIdAndStatus(
                    subjectId, status.trim().toUpperCase());
        }

        return assignmentRepository.findBySubjectId(subjectId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Assignment createAssignment(
            @PathVariable Long subjectId,
            @RequestBody Assignment assignment,
            Authentication authentication) {

        User user = getLoggedInUser(authentication);
        Subject subject = getOwnedSubject(subjectId, user.getId());

        if (assignment.getTitle() == null
                || assignment.getTitle().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Assignment title is required");
        }

        if (assignment.getStatus() == null
                || assignment.getStatus().isBlank()) {
            assignment.setStatus("PENDING");
        } else {
            assignment.setStatus(
                    assignment.getStatus().trim().toUpperCase());
        }

        if (!List.of("PENDING", "COMPLETED", "SUBMITTED")
                .contains(assignment.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status must be PENDING, COMPLETED, or SUBMITTED");
        }

        assignment.setSubjectId(subjectId);

        // Save the assignment first.
        Assignment savedAssignment = assignmentRepository.save(assignment);

        // Find the semester that owns this subject.
        Semester semester = semesterRepository.findById(subject.getSemesterId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Semester not found"));

        // Notify the user who owns the semester.
        notificationService.createNotification(
                semester.getUserId(),
                "New assignment added",
                "Assignment \"" + savedAssignment.getTitle()
                        + "\" has been added.",
                "ASSIGNMENT_CREATED"
        );

        return savedAssignment;
    }

    private User getLoggedInUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));
    }

    private Subject getOwnedSubject(Long subjectId, Long userId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Subject not found"));

        semesterRepository.findById(subject.getSemesterId())
                .filter(semester -> semester.getUserId().equals(userId))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Subject not found"));

        return subject;
    }
}
