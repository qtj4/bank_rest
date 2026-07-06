package com.example.bankcards.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @Schema(example = "admin")
    @NotBlank(message = "{validation.username.required}")
    private String username;

    @Schema(example = "admin123")
    @NotBlank(message = "{validation.password.required}")
    private String password;
}
