
package com.smartsem.smartsem.repository;

import com.smartsem.smartsem.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository
        extends JpaRepository<AttendanceRecord, Long> {

    List<AttendanceRecord> findBySubjectId(Long subjectId);

    List<AttendanceRecord> findBySubjectIdOrderByClassDateDesc(
            Long subjectId
    );

    List<AttendanceRecord> findBySubjectIdAndClassDate(
            Long subjectId,
            LocalDate classDate
    );

    // Find attendance records for one specific timetable slot.
    List<AttendanceRecord> findByTimetableEntryId(Long timetableEntryId);

    // Check whether this slot already has an attendance record on this date.
    Optional<AttendanceRecord> findByTimetableEntryIdAndClassDate(
            Long timetableEntryId,
            LocalDate classDate
    );
}
