
package com.smartsem.smartsem.controller;

import com.smartsem.smartsem.dto.TimetableImportRequest;
import com.smartsem.smartsem.entity.Semester;
import com.smartsem.smartsem.entity.Subject;
import com.smartsem.smartsem.entity.TimetableEntry;
import com.smartsem.smartsem.entity.User;
import com.smartsem.smartsem.repository.SemesterRepository;
import com.smartsem.smartsem.repository.SubjectRepository;
import com.smartsem.smartsem.repository.TimetableEntryRepository;
import com.smartsem.smartsem.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/subjects/{subjectId}/timetable")
public class TimetableEntryController {

    private final TimetableEntryRepository timetableRepository;
    private final SubjectRepository subjectRepository;
    private final SemesterRepository semesterRepository;
    private final UserRepository userRepository;

    public TimetableEntryController(
            TimetableEntryRepository timetableRepository,
            SubjectRepository subjectRepository,
            SemesterRepository semesterRepository,
            UserRepository userRepository) {
        this.timetableRepository = timetableRepository;
        this.subjectRepository = subjectRepository;
        this.semesterRepository = semesterRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<TimetableEntry> getTimetable(
            @PathVariable Long subjectId,
            Authentication authentication) {

        User user = getLoggedInUser(authentication);
        getOwnedSubject(subjectId, user.getId());

        return timetableRepository.findBySubjectId(subjectId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TimetableEntry createTimetableEntry(
            @PathVariable Long subjectId,
            @RequestBody TimetableEntry entry,
            Authentication authentication) {

        User user = getLoggedInUser(authentication);
        getOwnedSubject(subjectId, user.getId());

        validateEntry(
                entry.getDayOfWeek(),
                entry.getStartTime(),
                entry.getEndTime(),
                entry.getClassType());

        boolean exists = timetableEntryExists(
                subjectId,
                entry.getDayOfWeek(),
                entry.getStartTime(),
                entry.getEndTime(),
                entry.getGroup());

        if (exists) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This timetable entry already exists.");
        }

        entry.setSubjectId(subjectId);

        return timetableRepository.save(entry);
    }

    /*
     * Imports approved classes into the semester belonging to
     * the subject ID supplied in the URL.
     *
     * POST /api/subjects/{subjectId}/timetable/import
     */
    @PostMapping("/import")
    @Transactional
    public Map<String, Object> importTimetable(
            @PathVariable Long subjectId,
            @RequestBody List<TimetableImportRequest> entries,
            Authentication authentication) {

        User user = getLoggedInUser(authentication);

        Subject requestedSubject =
                getOwnedSubject(subjectId, user.getId());

        if (entries == null || entries.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "At least one timetable entry is required.");
        }

        if (entries.size() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Import at most 100 entries per request.");
        }

        Semester semester = semesterRepository
                .findById(requestedSubject.getSemesterId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Semester not found."));

        Set<String> batchKeys = new HashSet<>();
        List<TimetableEntry> toSave = new ArrayList<>();
        int skippedDuplicates = 0;

        // Validate and resolve all entries before saving.
        for (TimetableImportRequest request : entries) {

            if (request == null
                    || request.getSubjectName() == null
                    || request.getSubjectName().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Every entry must contain a subject name.");
            }

            validateEntry(
                    request.getDayOfWeek(),
                    request.getStartTime(),
                    request.getEndTime(),
                    request.getClassType());

            Subject subject = subjectRepository
                    .findBySemesterIdAndNameIgnoreCase(
                            semester.getId(),
                            request.getSubjectName().trim())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Unknown subject: "
                                    + request.getSubjectName()));

            String group = normalizeGroup(request.getGroup());

            String key = subject.getId() + "|"
                    + request.getDayOfWeek() + "|"
                    + request.getStartTime() + "|"
                    + request.getEndTime() + "|"
                    + (group == null ? "" : group);

            // Avoid duplicate entries within the same batch.
            if (!batchKeys.add(key)) {
                skippedDuplicates++;
                continue;
            }

            boolean exists = timetableEntryExists(
                    subject.getId(),
                    request.getDayOfWeek(),
                    request.getStartTime(),
                    request.getEndTime(),
                    group);

            if (exists) {
                skippedDuplicates++;
                continue;
            }

            TimetableEntry entry = new TimetableEntry();
            entry.setSubjectId(subject.getId());
            entry.setDayOfWeek(request.getDayOfWeek());
            entry.setStartTime(request.getStartTime());
            entry.setEndTime(request.getEndTime());
            entry.setRoom(request.getRoom());
            entry.setClassType(
                    request.getClassType().trim().toUpperCase());
            entry.setGroup(group);

            toSave.add(entry);
        }

        // Save only after all entries have been validated.
        timetableRepository.saveAll(toSave);

        return Map.of(
                "message", "Timetable import completed.",
                "saved", toSave.size(),
                "skippedDuplicates", skippedDuplicates);
    }

    private boolean timetableEntryExists(
            Long subjectId,
            Short dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            String group) {

        if (group == null) {
            return timetableRepository
                    .findBySubjectIdAndDayOfWeekAndStartTimeAndEndTime(
                            subjectId,
                            dayOfWeek,
                            startTime,
                            endTime)
                    .isPresent();
        }

        return timetableRepository
                .findBySubjectIdAndDayOfWeekAndStartTimeAndEndTimeAndGroup(
                        subjectId,
                        dayOfWeek,
                        startTime,
                        endTime,
                        group)
                .isPresent();
    }

    private String normalizeGroup(String group) {
        if (group == null || group.isBlank()) {
            return null;
        }
        return group.trim();
    }

    private void validateEntry(
            Short dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            String classType) {

        if (dayOfWeek == null
                || dayOfWeek < 1
                || dayOfWeek > 7
                || startTime == null
                || endTime == null
                || !endTime.isAfter(startTime)
                || classType == null
                || !Set.of("LECTURE", "LAB", "TUTORIAL")
                        .contains(classType.trim().toUpperCase())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid day, time range, or class type.");
        }
    }

    private User getLoggedInUser(Authentication authentication) {
        if (authentication == null
                || authentication.getName() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required.");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "User not found."));
    }

    private Subject getOwnedSubject(Long subjectId, Long userId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Subject not found."));

        semesterRepository.findById(subject.getSemesterId())
                .filter(s -> s.getUserId().equals(userId))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Subject not found."));

        return subject;
    }

    @DeleteMapping("/{entryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTimetableEntry(
            @PathVariable Long subjectId,
            @PathVariable Long entryId,
            Authentication authentication) {

        User user = getLoggedInUser(authentication);
        getOwnedSubject(subjectId, user.getId());

        TimetableEntry entry = timetableRepository.findById(entryId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Timetable entry not found."));

        if (!entry.getSubjectId().equals(subjectId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Timetable entry not found.");
        }

        timetableRepository.delete(entry);
    }
}
