package com.example.bankcards.dto.request;

import com.example.bankcards.entity.enums.Role;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {

    @Size(max = 255, message = "{validation.full-name.size}")
    private String fullName;

    @Size(min = 8, max = 72, message = "{validation.password.size}")
    private String password;

    private Role role;

    private Boolean enabled;
}
