
package com.smartsem.smartsem.dto;

public class AuthResponse {

    private String token;
    private Long userId;
    private String fullName;
    private String email;

    public AuthResponse() {
    }

    public AuthResponse(
            String token,
            Long userId,
            String fullName,
            String email) {
        this.token = token;
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
    }

    public String getToken() {
        return token;
    }

    public Long getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }
}
