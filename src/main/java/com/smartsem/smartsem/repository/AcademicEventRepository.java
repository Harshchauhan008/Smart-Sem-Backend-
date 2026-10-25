
package com.smartsem.smartsem.repository;

import com.smartsem.smartsem.entity.AcademicEvent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface AcademicEventRepository
        extends JpaRepository<AcademicEvent, Long> {

    // Get all events for a semester
    List<AcademicEvent> findBySemesterId(Long semesterId);


    // Get events of a particular type in a semester
    List<AcademicEvent> findBySemesterIdAndEventType(
            Long semesterId,
            String eventType
    );
}
