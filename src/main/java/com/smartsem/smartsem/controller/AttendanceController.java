
package com.smartsem.smartsem.controller;

import com.smartsem.smartsem.entity.AttendanceRecord;
import com.smartsem.smartsem.entity.Semester;
import com.smartsem.smartsem.entity.Subject;
import com.smartsem.smartsem.entity.TimetableEntry;
import com.smartsem.smartsem.entity.User;
import com.smartsem.smartsem.repository.SemesterRepository;
import com.smartsem.smartsem.repository.SubjectRepository;
import com.smartsem.smartsem.repository.TimetableEntryRepository;
import com.smartsem.smartsem.repository.UserRepository;
import com.smartsem.smartsem.service.AttendanceRecordService;
import com.smartsem.smartsem.service.NotificationService;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subjects/{subjectId}/attendance")
public class AttendanceController {

    private final AttendanceRecordService attendanceService;
    private final SemesterRepository semesterRepository;
    private final SubjectRepository subjectRepository;
    private final TimetableEntryRepository timetableEntryRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public AttendanceController(
            AttendanceRecordService attendanceService,
            SemesterRepository semesterRepository,
            SubjectRepository subjectRepository,
            TimetableEntryRepository timetableEntryRepository,
            UserRepository userRepository,
            NotificationService notificationService) {

        this.attendanceService = attendanceService;
        this.semesterRepository = semesterRepository;
        this.subjectRepository = subjectRepository;
        this.timetableEntryRepository = timetableEntryRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    private User getAuthenticatedUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));
    }

    private Subject getOwnedSubject(
            Long subjectId,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        Semester semester = semesterRepository
                .findByUserIdAndActiveTrue(user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "No active semester found."));

        return subjectRepository.findById(subjectId)
                .filter(subject ->
                        subject.getSemesterId().equals(semester.getId()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Subject not found in your active semester."));
    }

    // Mark attendance for a specific timetable slot.
    @PostMapping
    public ResponseEntity<?> markAttendance(
            @PathVariable Long subjectId,
            @RequestBody AttendanceRecord request,
            Authentication authentication) {

        Subject subject = getOwnedSubject(subjectId, authentication);

        if (request.getClassDate() == null
                || request.getStatus() == null
                || request.getStatus().isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "classDate and status are required."));
        }

        String status = request.getStatus().trim().toUpperCase();

        if (!List.of("PRESENT", "ABSENT").contains(status)) {
            return ResponseEntity.badRequest().body(
                    Map.of("message",
                            "Status must be PRESENT or ABSENT. Use /cancelled to cancel a class."));
        }

        Long timetableEntryId = request.getTimetableEntryId();

        if (timetableEntryId == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "timetableEntryId is required."));
        }

        validateTimetableEntry(timetableEntryId, subjectId);

        boolean alreadyRecorded = attendanceService
                .getAttendanceBySubject(subjectId)
                .stream()
                .anyMatch(record ->
                        request.getClassDate().equals(record.getClassDate())
                                && timetableEntryId.equals(
                                        record.getTimetableEntryId()));

        if (alreadyRecorded) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("message",
                            "A record already exists for this timetable slot and date."));
        }

        AttendanceRecord record = new AttendanceRecord();
        record.setSubjectId(subjectId);
        record.setTimetableEntryId(timetableEntryId);
        record.setClassDate(request.getClassDate());
        record.setStatus(status);
        record.setNotes(request.getNotes());

        AttendanceRecord savedRecord =
                attendanceService.createAttendance(record);

        User user = getAuthenticatedUser(authentication);

        notificationService.createNotification(
                user.getId(),
                "Attendance marked",
                "Your attendance for " + subject.getName()
                        + " on " + savedRecord.getClassDate()
                        + " was marked " + savedRecord.getStatus() + ".",
                "ATTENDANCE_MARKED"
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(savedRecord);
    }

    // Cancel one specific timetable slot on a given date.
    @PostMapping("/cancelled")
    public ResponseEntity<?> markClassCancelled(
            @PathVariable Long subjectId,
            @RequestBody Map<String, Object> request,
            Authentication authentication) {

        Subject subject = getOwnedSubject(subjectId, authentication);

        Object dateValue = request.get("classDate");
        Object slotValue = request.get("timetableEntryId");

        if (dateValue == null || slotValue == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("message",
                            "classDate and timetableEntryId are required."));
        }

        LocalDate classDate;
        Long timetableEntryId;

        try {
            classDate = LocalDate.parse(dateValue.toString());
            timetableEntryId = Long.valueOf(slotValue.toString());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message",
                            "Provide a valid YYYY-MM-DD classDate and numeric timetableEntryId."));
        }

        if (timetableEntryId <= 0) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "timetableEntryId must be positive."));
        }

        validateTimetableEntry(timetableEntryId, subjectId);

        String notes = request.get("notes") == null
                ? "Class did not happen"
                : request.get("notes").toString().trim();

        if (notes.isBlank()) {
            notes = "Class did not happen";
        }

        boolean alreadyRecorded = attendanceService
                .getAttendanceBySubject(subjectId)
                .stream()
                .anyMatch(record ->
                        classDate.equals(record.getClassDate())
                                && timetableEntryId.equals(
                                        record.getTimetableEntryId()));

        if (alreadyRecorded) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("message",
                            "A record already exists for this timetable slot and date."));
        }

        try {
            AttendanceRecord savedRecord =
                    attendanceService.recordCancelledClass(
                            subjectId,
                            timetableEntryId,
                            classDate,
                            notes);

            User user = getAuthenticatedUser(authentication);

            notificationService.createNotification(
                    user.getId(),
                    "Class marked as cancelled",
                    subject.getName() + " on " + classDate
                            + " was marked: " + notes,
                    "CLASS_CANCELLED"
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(savedRecord);

        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("message",
                            "A record already exists for this timetable slot and date."));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("message", e.getMessage()));
        }
    }

    private void validateTimetableEntry(
            Long timetableEntryId,
            Long subjectId) {

        TimetableEntry entry = timetableEntryRepository
                .findById(timetableEntryId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Timetable entry not found."));

        if (!subjectId.equals(entry.getSubjectId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The timetable entry does not belong to this subject.");
        }
    }

    @GetMapping
    public ResponseEntity<List<AttendanceRecord>> getAttendance(
            @PathVariable Long subjectId,
            Authentication authentication) {

        getOwnedSubject(subjectId, authentication);

        return ResponseEntity.ok(
                attendanceService.getAttendanceBySubject(subjectId));
    }

    @GetMapping("/percentage")
    public ResponseEntity<?> getAttendancePercentage(
            @PathVariable Long subjectId,
            Authentication authentication) {

        getOwnedSubject(subjectId, authentication);

        double percentage =
                attendanceService.calculateAttendancePercentage(subjectId);

        return ResponseEntity.ok(
                Map.of(
                        "subjectId", subjectId,
                        "attendancePercentage", percentage
                )
        );
    }
}
