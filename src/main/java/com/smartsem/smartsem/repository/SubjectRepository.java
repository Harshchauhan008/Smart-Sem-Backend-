
package com.smartsem.smartsem.repository;

import com.smartsem.smartsem.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubjectRepository
        extends JpaRepository<Subject, Long> {

    List<Subject> findBySemesterId(Long semesterId);

    Optional<Subject> findByIdAndSemesterId(
            Long id,
            Long semesterId
    );

    Optional<Subject> findBySemesterIdAndNameIgnoreCase(
            Long semesterId,
            String name
    );
}
