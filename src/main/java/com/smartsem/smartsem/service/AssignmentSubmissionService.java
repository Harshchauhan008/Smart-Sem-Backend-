
package com.smartsem.smartsem.service;

import com.smartsem.smartsem.entity.AssignmentSubmission;
import com.smartsem.smartsem.repository.AssignmentSubmissionRepository;

import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class AssignmentSubmissionService {

    private final AssignmentSubmissionRepository submissionRepository;


    public AssignmentSubmissionService(
            AssignmentSubmissionRepository submissionRepository) {

        this.submissionRepository = submissionRepository;
    }



    // Get all submissions for an assignment
    public List<AssignmentSubmission> getSubmissionsByAssignment(
            Long assignmentId) {

        return submissionRepository.findByAssignmentId(assignmentId);
    }



    // Save a submission record
    public AssignmentSubmission createSubmission(
            AssignmentSubmission submission) {

        return submissionRepository.save(submission);
    }
}
