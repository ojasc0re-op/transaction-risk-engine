package com.frauddetection.dto.response;

import java.util.Set;
import java.util.UUID;

public record AuthResponse(
        String token,
        String tokenType,
        String username,
        String email,
        UUID userId,
        Set<String> roles
) {
    public static AuthResponse of(String token, String username, String email,
                                  UUID userId, Set<String> roles) {
        return new AuthResponse(token, "Bearer", username, email, userId, roles);
    }
}
