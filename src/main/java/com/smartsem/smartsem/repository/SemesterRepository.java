
package com.smartsem.smartsem.repository;

import com.smartsem.smartsem.entity.Semester;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SemesterRepository
        extends JpaRepository<Semester, Long> {

    List<Semester> findByUserId(Long userId);

    Optional<Semester> findByIdAndUserId(
            Long id, Long userId);

    Optional<Semester> findByUserIdAndActiveTrue(
            Long userId);

    @Modifying(
        clearAutomatically = true,
        flushAutomatically = true
    )
    @Query("""
        UPDATE Semester s
        SET s.active = false
        WHERE s.userId = :userId
        """)
    int deactivateAllByUserId(
            @Param("userId") Long userId);
}
