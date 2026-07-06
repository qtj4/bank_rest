package com.example.bankcards.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @Schema(example = "ivan")
    @NotBlank(message = "{validation.username.required}")
    @Size(max = 255, message = "{validation.username.size}")
    private String username;

    @Schema(example = "StrongPassword1")
    @NotBlank(message = "{validation.password.required}")
    @Size(min = 8, max = 72, message = "{validation.password.size}")
    private String password;

    @Schema(example = "Ivan Petrov")
    @NotBlank(message = "{validation.full-name.required}")
    @Size(max = 255, message = "{validation.full-name.size}")
    private String fullName;
}
