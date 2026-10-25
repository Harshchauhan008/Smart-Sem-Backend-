
package com.smartsem.smartsem.repository;

import com.smartsem.smartsem.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface AssignmentRepository
        extends JpaRepository<Assignment, Long> {

    List<Assignment> findBySubjectId(Long subjectId);

    List<Assignment> findBySubjectIdAndStatus(
            Long subjectId,
            String status
    );

    List<Assignment> findByDeadlineBetweenAndStatusIn(
            OffsetDateTime start,
            OffsetDateTime end,
            List<String> statuses
    );
}
