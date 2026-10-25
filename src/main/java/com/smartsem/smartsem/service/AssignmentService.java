
package com.smartsem.smartsem.service;

import com.smartsem.smartsem.entity.Assignment;
import com.smartsem.smartsem.repository.AssignmentRepository;

import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;


    public AssignmentService(
            AssignmentRepository assignmentRepository) {

        this.assignmentRepository = assignmentRepository;
    }



    // Get all assignments for a subject
    public List<Assignment> getAssignmentsBySubject(
            Long subjectId) {

        return assignmentRepository.findBySubjectId(subjectId);
    }



    // Get assignments filtered by status
    public List<Assignment> getAssignmentsByStatus(
            Long subjectId,
            String status) {

        return assignmentRepository
                .findBySubjectIdAndStatus(subjectId, status);
    }



    // Create an assignment
    public Assignment createAssignment(Assignment assignment) {

        return assignmentRepository.save(assignment);
    }
}
