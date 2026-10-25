
package com.smartsem.smartsem.controller;

import com.smartsem.smartsem.entity.User;
import com.smartsem.smartsem.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public Map<String, Object> getCurrentUser(
            Authentication authentication) {

        User user = userRepository.findByEmail(
                authentication.getName()
        ).orElseThrow(() ->
                new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                )
        );

        return Map.of(
                "id", user.getId(),
                "fullName", user.getFullName(),
                "email", user.getEmail()
        );
    }
}
