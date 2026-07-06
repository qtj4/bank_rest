package com.example.bankcards.dto.response;

import com.example.bankcards.entity.enums.Role;
import java.util.UUID;

public record AuthResponse(
        String token,
        String tokenType,
        UUID userId,
        String username,
        Role role
) {
}
