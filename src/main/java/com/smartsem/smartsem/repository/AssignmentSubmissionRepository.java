
package com.smartsem.smartsem.repository;

import com.smartsem.smartsem.entity.AssignmentSubmission;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface AssignmentSubmissionRepository
        extends JpaRepository<AssignmentSubmission, Long> {

    // Get all submissions for an assignment
    List<AssignmentSubmission> findByAssignmentId(Long assignmentId);
}
