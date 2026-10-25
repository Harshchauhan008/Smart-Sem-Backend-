
package com.smartsem.smartsem.controller;

import com.smartsem.smartsem.entity.Semester;
import com.smartsem.smartsem.entity.User;
import com.smartsem.smartsem.repository.SemesterRepository;
import com.smartsem.smartsem.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/semesters")
public class SemesterController {

    private final SemesterRepository semesterRepository;
    private final UserRepository userRepository;

    public SemesterController(
            SemesterRepository semesterRepository,
            UserRepository userRepository) {
        this.semesterRepository = semesterRepository;
        this.userRepository = userRepository;
    }

    // Get all semesters belonging to the logged-in user
    @GetMapping
    public List<Semester> getMySemesters(
            Authentication authentication) {

        User user = getLoggedInUser(authentication);

        return semesterRepository.findByUserId(user.getId());
    }

    // Create a semester and make it the active semester
    @Transactional
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Semester createSemester(
            @RequestBody Semester semester,
            Authentication authentication) {

        User user = getLoggedInUser(authentication);

        // Deactivate the user's previous semesters
        semesterRepository.deactivateAllByUserId(user.getId());

        // Never trust a user ID supplied by the frontend
        
        semester.setUserId(user.getId());
        semester.setActive(true);

        return semesterRepository.save(semester);
    }

    // Activate an existing semester
    @Transactional
    @PutMapping("/{semesterId}/activate")
    public Semester activateSemester(
            @PathVariable Long semesterId,
            Authentication authentication) {

        User user = getLoggedInUser(authentication);

        Semester semester = semesterRepository
                .findByIdAndUserId(semesterId, user.getId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Semester not found"
                        )
                );

        // Deactivate all semesters belonging to this user
        semesterRepository.deactivateAllByUserId(user.getId());

        // Reload after the bulk update to avoid stale entity state
        semester = semesterRepository
                .findByIdAndUserId(semesterId, user.getId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Semester not found"
                        )
                );

        semester.setActive(true);

        return semesterRepository.save(semester);
    }

    // Find the currently authenticated user
    private User getLoggedInUser(Authentication authentication) {

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        )
                );
    }
}
