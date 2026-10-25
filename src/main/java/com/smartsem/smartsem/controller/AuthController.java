
package com.smartsem.smartsem.controller;

import com.smartsem.smartsem.dto.AuthResponse;
import com.smartsem.smartsem.dto.LoginRequest;
import com.smartsem.smartsem.dto.RegisterRequest;
import com.smartsem.smartsem.entity.User;
import com.smartsem.smartsem.repository.UserRepository;
import com.smartsem.smartsem.security.JwtService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest request) {

        String email = request.getEmail()
                .trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("An account with this email already exists.");
        }

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPasswordHash(
                passwordEncoder.encode(request.getPassword()));

        User savedUser = userRepository.save(user);

        String token = jwtService.generateToken(
                savedUser.getId(), savedUser.getEmail());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(
                        token,
                        savedUser.getId(),
                        savedUser.getFullName(),
                        savedUser.getEmail()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request) {

        String email = request.getEmail()
                .trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email)
                .orElse(null);

        if (user == null ||
                !passwordEncoder.matches(
                        request.getPassword(),
                        user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid email or password.");
        }

        String token = jwtService.generateToken(
                user.getId(), user.getEmail());

        return ResponseEntity.ok(
                new AuthResponse(
                        token,
                        user.getId(),
                        user.getFullName(),
                        user.getEmail()));
    }

    @PostMapping("/change-password")
public ResponseEntity<?> changePassword(
        @RequestBody java.util.Map<String, String> request,
        Authentication authentication) {

    if (authentication == null || !authentication.isAuthenticated()) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Please log in again.");
    }

    String email = authentication.getName();

    String currentPassword = request.get("currentPassword");
    String newPassword = request.get("newPassword");

    if (currentPassword == null || newPassword == null
            || currentPassword.isBlank() || newPassword.isBlank()) {
        return ResponseEntity.badRequest()
                .body("Current password and new password are required.");
    }

    if (newPassword.length() < 8) {
        return ResponseEntity.badRequest()
                .body("New password must be at least 8 characters.");
    }

    User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.NOT_FOUND, "User not found."));

    if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Current password is incorrect.");
    }

    if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
        return ResponseEntity.badRequest()
                .body("New password must differ from the current password.");
    }

    user.setPasswordHash(passwordEncoder.encode(newPassword));
    userRepository.save(user);

    return ResponseEntity.ok(
            java.util.Map.of("message", "Password changed successfully."));
}
}
