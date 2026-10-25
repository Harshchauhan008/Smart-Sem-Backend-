
package com.smartsem.smartsem.repository;

import com.smartsem.smartsem.entity.TimetableEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface TimetableEntryRepository
        extends JpaRepository<TimetableEntry, Long> {

    List<TimetableEntry> findBySubjectId(Long subjectId);

    Optional<TimetableEntry>
    findBySubjectIdAndDayOfWeekAndStartTimeAndEndTime(
            Long subjectId,
            Short dayOfWeek,
            java.time.LocalTime startTime,
            java.time.LocalTime endTime
    );

    
Optional<TimetableEntry>
findBySubjectIdAndDayOfWeekAndStartTimeAndEndTimeAndGroup(
        Long subjectId,
        Short dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        String group);


}
