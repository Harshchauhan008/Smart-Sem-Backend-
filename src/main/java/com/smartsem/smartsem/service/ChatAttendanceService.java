package com.smartsem.smartsem.service;

import com.smartsem.smartsem.entity.AttendanceRecord;
import com.smartsem.smartsem.entity.Semester;
import com.smartsem.smartsem.entity.Subject;
import com.smartsem.smartsem.entity.TimetableEntry;
import com.smartsem.smartsem.repository.AttendanceRecordRepository;
import com.smartsem.smartsem.repository.SemesterRepository;
import com.smartsem.smartsem.repository.SubjectRepository;
import com.smartsem.smartsem.repository.TimetableEntryRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

@Service
public class ChatAttendanceService {

    private final SemesterRepository semesterRepository;
    private final SubjectRepository subjectRepository;
    private final TimetableEntryRepository timetableRepository;
    private final AttendanceRecordRepository attendanceRepository;

    public ChatAttendanceService(
            SemesterRepository semesterRepository,
            SubjectRepository subjectRepository,
            TimetableEntryRepository timetableRepository,
            AttendanceRecordRepository attendanceRepository) {

        this.semesterRepository = semesterRepository;
        this.subjectRepository = subjectRepository;
        this.timetableRepository = timetableRepository;
        this.attendanceRepository = attendanceRepository;
    }

    @Transactional
    public String markAttendance(
            Long userId,
            Long semesterId,
            String subjectName,
            LocalDate classDate,
            String requestedStatus,
            LocalTime requestedStartTime) {

        if (semesterId == null) {
            return "Please select a semester before marking attendance.";
        }

        if (subjectName == null || subjectName.isBlank()) {
            return "Which subject should I mark attendance for?";
        }

        if (classDate == null) {
            return "Which date should I use for the attendance record?";
        }

        if (classDate.isAfter(LocalDate.now())) {
            return "Attendance cannot be marked for a future date.";
        }

        String status = requestedStatus == null
                ? ""
                : requestedStatus.trim().toUpperCase(Locale.ROOT);

        if (!status.equals("PRESENT") && !status.equals("ABSENT")) {
            return "Should I mark you PRESENT or ABSENT?";
        }

        Semester semester = semesterRepository.findById(semesterId)
                .orElse(null);

        if (semester == null
                || !userId.equals(semester.getUserId())) {
            return "I couldn't verify your semester. Please select it again.";
        }

        List<Subject> subjects =
                subjectRepository.findBySemesterId(semesterId);

        String searchName = subjectName.trim();

        List<Subject> matches = subjects.stream()
                .filter(subject ->
                        subject.getName().equalsIgnoreCase(searchName)
                        || (subject.getSubjectCode() != null
                        && subject.getSubjectCode()
                                .equalsIgnoreCase(searchName)))
                .toList();

        if (matches.isEmpty()) {
            return "I couldn't find the subject '" + searchName
                    + "' in your selected semester. Please provide its exact name.";
        }

        if (matches.size() > 1) {
            return "I found more than one matching subject. Please specify the subject name or code.";
        }

        Subject subject = matches.get(0);

        // Java DayOfWeek: Monday = 1, Sunday = 7.
        short dayOfWeek = (short) classDate.getDayOfWeek().getValue();

        List<TimetableEntry> classes =
                timetableRepository.findBySubjectId(subject.getId())
                        .stream()
                        .filter(entry ->
                                entry.getDayOfWeek() != null
                                && entry.getDayOfWeek() == dayOfWeek)
                        .filter(entry ->
                                requestedStartTime == null
                                || entry.getStartTime()
                                        .equals(requestedStartTime))
                        .toList();

        if (classes.isEmpty()) {
            return "I couldn't find a scheduled " + subject.getName()
                    + " class for " + classDate
                    + ". Please check the date or timetable.";
        }

        if (classes.size() > 1) {
            return "You have multiple " + subject.getName()
                    + " classes that day. Please specify the class start time.";
        }

        TimetableEntry timetableEntry = classes.get(0);

        var existing = attendanceRepository
                .findByTimetableEntryIdAndClassDate(
                        timetableEntry.getId(), classDate);

        if (existing.isPresent()) {
            String existingStatus = existing.get().getStatus();

            if ("CANCELLED".equalsIgnoreCase(existingStatus)) {
                return "This class is recorded as cancelled, so I won't mark attendance for it.";
            }

            return "Attendance for " + subject.getName()
                    + " on " + classDate
                    + " is already recorded as "
                    + existingStatus + ".";
        }

        AttendanceRecord record = new AttendanceRecord();
        record.setSubjectId(subject.getId());
        record.setTimetableEntryId(timetableEntry.getId());
        record.setClassDate(classDate);
        record.setStatus(status);
        record.setNotes("Recorded through SmartSem chat");

        attendanceRepository.save(record);

        return "Done! I marked you " + status + " for "
                + subject.getName() + " on " + classDate
                + " (" + timetableEntry.getStartTime()
                + "–" + timetableEntry.getEndTime() + ").";
    }
}
