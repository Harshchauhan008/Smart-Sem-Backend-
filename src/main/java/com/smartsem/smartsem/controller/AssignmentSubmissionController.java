
package com.smartsem.smartsem.controller;

import com.smartsem.smartsem.entity.Assignment;
import com.smartsem.smartsem.entity.AssignmentSubmission;
import com.smartsem.smartsem.entity.Semester;
import com.smartsem.smartsem.entity.Subject;
import com.smartsem.smartsem.entity.User;

import com.smartsem.smartsem.repository.AssignmentRepository;
import com.smartsem.smartsem.repository.AssignmentSubmissionRepository;
import com.smartsem.smartsem.repository.SemesterRepository;
import com.smartsem.smartsem.repository.SubjectRepository;
import com.smartsem.smartsem.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;


@RestController
@RequestMapping("/api/assignments/{assignmentId}/submissions")
public class AssignmentSubmissionController {

    private final AssignmentSubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubjectRepository subjectRepository;
    private final SemesterRepository semesterRepository;
    private final UserRepository userRepository;


    public AssignmentSubmissionController(
            AssignmentSubmissionRepository submissionRepository,
            AssignmentRepository assignmentRepository,
            SubjectRepository subjectRepository,
            SemesterRepository semesterRepository,
            UserRepository userRepository) {

        this.submissionRepository = submissionRepository;
        this.assignmentRepository = assignmentRepository;
        this.subjectRepository = subjectRepository;
        this.semesterRepository = semesterRepository;
        this.userRepository = userRepository;
    }



    // Get submissions for an assignment owned by the logged-in user
    @GetMapping
    public List<AssignmentSubmission> getSubmissions(
            @PathVariable Long assignmentId,
            Authentication authentication) {

        User user = getLoggedInUser(authentication);

        getOwnedAssignment(assignmentId, user.getId());

        return submissionRepository.findByAssignmentId(assignmentId);
    }



    // Save submission metadata for an owned assignment
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssignmentSubmission createSubmission(
            @PathVariable Long assignmentId,
            @RequestBody AssignmentSubmission submission,
            Authentication authentication) {

        User user = getLoggedInUser(authentication);

        Assignment assignment =
                getOwnedAssignment(assignmentId, user.getId());

        if (submission.getFileName() == null
                || submission.getFileName().isBlank()
                || submission.getFilePath() == null
                || submission.getFilePath().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "File name and file path are required"
            );
        }

        // Use the assignment ID from the URL
        submission.setAssignmentId(assignment.getId());

        AssignmentSubmission savedSubmission =
                submissionRepository.save(submission);

        // Update the assignment status
        assignment.setStatus("SUBMITTED");
        assignmentRepository.save(assignment);

        return savedSubmission;
    }



    // Find the logged-in user
    private User getLoggedInUser(Authentication authentication) {

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        )
                );
    }



    // Verify assignment ownership through subject and semester
    private Assignment getOwnedAssignment(
            Long assignmentId,
            Long userId) {

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Assignment not found"
                        )
                );

        Subject subject = subjectRepository.findById(
                assignment.getSubjectId()
        ).orElseThrow(() ->
                new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Assignment not found"
                )
        );

        semesterRepository.findById(subject.getSemesterId())
                .filter(semester -> semester.getUserId().equals(userId))
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Assignment not found"
                        )
                );

        return assignment;
    }
}
