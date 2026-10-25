
package com.smartsem.smartsem.controller;

import com.smartsem.smartsem.entity.AcademicEvent;
import com.smartsem.smartsem.entity.User;

import com.smartsem.smartsem.repository.SemesterRepository;
import com.smartsem.smartsem.repository.UserRepository;

import com.smartsem.smartsem.service.AcademicEventService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/semesters/{semesterId}/events")
public class AcademicEventController {

    private final AcademicEventService eventService;
    private final SemesterRepository semesterRepository;
    private final UserRepository userRepository;


    public AcademicEventController(
            AcademicEventService eventService,
            SemesterRepository semesterRepository,
            UserRepository userRepository) {

        this.eventService = eventService;
        this.semesterRepository = semesterRepository;
        this.userRepository = userRepository;
    }


    // Verify that the semester belongs to the logged-in user
    private boolean ownsSemester(
            Long semesterId,
            Authentication authentication) {

        User user = userRepository
                .findByEmail(authentication.getName())
                .orElse(null);

        if (user == null) {
            return false;
        }

        return semesterRepository.findById(semesterId)
                .map(semester ->
                        semester.getUserId().equals(user.getId()))
                .orElse(false);
    }


    // GET: Retrieve events, optionally filtered by type
    @GetMapping
    public ResponseEntity<?> getEvents(
            @PathVariable Long semesterId,
            @RequestParam(required = false) String type,
            Authentication authentication) {

        if (!ownsSemester(semesterId, authentication)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Semester not found"));
        }

        List<AcademicEvent> events;

        if (type != null && !type.isBlank()) {
            events = eventService.getEventsByType(
                    semesterId,
                    type.trim().toUpperCase()
            );
        } else {
            events = eventService.getEventsBySemester(semesterId);
        }

        return ResponseEntity.ok(events);
    }


    // POST: Create an academic event
    @PostMapping
    public ResponseEntity<?> createEvent(
            @PathVariable Long semesterId,
            @RequestBody AcademicEvent event,
            Authentication authentication) {

        if (!ownsSemester(semesterId, authentication)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Semester not found"));
        }

        if (event.getTitle() == null
                || event.getTitle().isBlank()
                || event.getEventType() == null
                || event.getEventType().isBlank()) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Title and event type are required"
                    ));
        }

        if (event.getStartsAt() != null
                && event.getEndsAt() != null
                && event.getEndsAt().isBefore(event.getStartsAt())) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Event end time cannot be before start time"
                    ));
        }

        event.setSemesterId(semesterId);
        event.setEventType(
                event.getEventType().trim().toUpperCase()
        );

        AcademicEvent savedEvent =
                eventService.createEvent(event);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savedEvent);
    }
}
