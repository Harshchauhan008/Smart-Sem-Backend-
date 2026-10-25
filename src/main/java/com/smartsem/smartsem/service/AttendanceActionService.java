
package com.smartsem.smartsem.service;

import com.smartsem.smartsem.dto.IntentExtractionResponse;
import com.smartsem.smartsem.entity.AttendanceRecord;
import com.smartsem.smartsem.entity.Semester;
import com.smartsem.smartsem.entity.Subject;
import com.smartsem.smartsem.entity.User;
import com.smartsem.smartsem.repository.AttendanceRecordRepository;
import com.smartsem.smartsem.repository.SemesterRepository;
import com.smartsem.smartsem.repository.SubjectRepository;
import com.smartsem.smartsem.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class AttendanceActionService {

    private final IntentExtractionService intentExtractionService;
    private final AttendanceRecordRepository attendanceRepository;
    private final SemesterRepository semesterRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;

    public AttendanceActionService(
            IntentExtractionService intentExtractionService,
            AttendanceRecordRepository attendanceRepository,
            SemesterRepository semesterRepository,
            SubjectRepository subjectRepository,
            UserRepository userRepository) {

        this.intentExtractionService = intentExtractionService;
        this.attendanceRepository = attendanceRepository;
        this.semesterRepository = semesterRepository;
        this.subjectRepository = subjectRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AttendanceRecord markAttendanceFromMessage(
            String message,
            Authentication authentication) {

        IntentExtractionResponse intent =
                intentExtractionService.extractIntent(message);

        if (!"MARK_ATTENDANCE".equals(intent.getIntent())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This message is not an attendance request."
            );
        }

        if (intent.getSubjectName() == null
                || intent.getSubjectName().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Please specify the subject name."
            );
        }

        if (intent.getClassDate() == null
                || intent.getClassDate().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Please specify the class date."
            );
        }

        if ("LOW".equalsIgnoreCase(intent.getConfidence())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    intent.getClarificationQuestion() != null
                            ? intent.getClarificationQuestion()
                            : "Please clarify your attendance request."
            );
        }

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        Semester semester = semesterRepository
                .findByUserIdAndActiveTrue(user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "No active semester found. Please activate a semester first."));

        Subject subject = subjectRepository
                .findBySemesterIdAndNameIgnoreCase(
                        semester.getId(),
                        intent.getSubjectName().trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Subject not found in your active semester: "
                                + intent.getSubjectName()));

        LocalDate classDate;

        try {
            classDate = LocalDate.parse(intent.getClassDate());
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The extracted class date is invalid. Please clarify the date.");
        }

        List<AttendanceRecord> existingRecords =
                attendanceRepository.findBySubjectIdAndClassDate(
                        subject.getId(), classDate);

        if (!existingRecords.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Attendance has already been recorded for "
                            + subject.getName() + " on " + classDate);
        }

        AttendanceRecord record = new AttendanceRecord();
        record.setSubjectId(subject.getId());
        record.setClassDate(classDate);
        record.setStatus("PRESENT");
        record.setNotes("Marked through SmartSem AI chat.");

        return attendanceRepository.save(record);
    }
}
