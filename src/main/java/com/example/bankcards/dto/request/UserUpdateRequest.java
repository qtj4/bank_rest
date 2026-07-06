package com.example.bankcards.dto.request;

import com.example.bankcards.entity.enums.Role;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @Size(max = 255, message = "Full name must be at most 255 characters")
        String fullName,

        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        String password,

        Role role,

        Boolean enabled
) {
}
