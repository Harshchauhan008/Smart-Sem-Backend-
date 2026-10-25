
package com.smartsem.smartsem.service;

import com.smartsem.smartsem.entity.AttendanceRecord;
import com.smartsem.smartsem.repository.AttendanceRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AttendanceRecordService {

    private final AttendanceRecordRepository attendanceRepository;

    public AttendanceRecordService(
            AttendanceRecordRepository attendanceRepository) {
        this.attendanceRepository = attendanceRepository;
    }

    public List<AttendanceRecord> getAttendanceBySubject(Long subjectId) {
        return attendanceRepository
                .findBySubjectIdOrderByClassDateDesc(subjectId);
    }

    @Transactional
    public AttendanceRecord createAttendance(
            AttendanceRecord attendanceRecord) {

        return attendanceRepository.save(attendanceRecord);
    }

    // Record a cancelled class for one specific timetable slot.
    @Transactional
    public AttendanceRecord recordCancelledClass(
            Long subjectId,
            Long timetableEntryId,
            LocalDate classDate,
            String notes) {

        if (attendanceRepository
                .findByTimetableEntryIdAndClassDate(
                        timetableEntryId, classDate)
                .isPresent()) {
            throw new IllegalStateException(
                    "Attendance has already been recorded for this class slot and date."
            );
        }

        AttendanceRecord record = new AttendanceRecord();
        record.setSubjectId(subjectId);
        record.setTimetableEntryId(timetableEntryId);
        record.setClassDate(classDate);
        record.setStatus("CANCELLED");
        record.setNotes(
                notes == null || notes.isBlank()
                        ? "Class did not happen"
                        : notes.trim()
        );

        return attendanceRepository.save(record);
    }

    // Backward-compatible method for existing callers.
    @Transactional
    public AttendanceRecord recordCancelledClass(
            Long subjectId,
            LocalDate classDate,
            String notes) {

        AttendanceRecord record = new AttendanceRecord();
        record.setSubjectId(subjectId);
        record.setClassDate(classDate);
        record.setStatus("CANCELLED");
        record.setNotes(
                notes == null || notes.isBlank()
                        ? "Class did not happen"
                        : notes.trim()
        );

        return attendanceRepository.save(record);
    }

    public double calculateAttendancePercentage(Long subjectId) {
        List<AttendanceRecord> records =
                attendanceRepository.findBySubjectId(subjectId);

        long totalClasses = records.stream()
                .filter(record ->
                        !"CANCELLED".equalsIgnoreCase(record.getStatus()))
                .count();

        if (totalClasses == 0) {
            return 0.0;
        }

        long presentClasses = records.stream()
                .filter(record ->
                        "PRESENT".equalsIgnoreCase(record.getStatus()))
                .count();

        return Math.round(
                (presentClasses * 10000.0) / totalClasses
        ) / 100.0;
    }

    
@Transactional
public AttendanceRecord recordAttendanceForClass(
        Long subjectId,
        Long timetableEntryId,
        LocalDate classDate,
        String status,
        String notes) {

    if (subjectId == null || timetableEntryId == null
            || classDate == null) {
        throw new IllegalArgumentException(
                "Subject, timetable slot, and date are required.");
    }

    if (status == null
            || (!"PRESENT".equalsIgnoreCase(status)
            && !"ABSENT".equalsIgnoreCase(status))) {
        throw new IllegalArgumentException(
                "Status must be PRESENT or ABSENT.");
    }

    if (attendanceRepository
            .findByTimetableEntryIdAndClassDate(
                    timetableEntryId, classDate)
            .isPresent()) {
        throw new IllegalStateException(
                "Attendance is already recorded for this class and date.");
    }

    AttendanceRecord record = new AttendanceRecord();
    record.setSubjectId(subjectId);
    record.setTimetableEntryId(timetableEntryId);
    record.setClassDate(classDate);
    record.setStatus(status.toUpperCase(java.util.Locale.ROOT));
    record.setNotes(notes);

    return attendanceRepository.save(record);
}

}
