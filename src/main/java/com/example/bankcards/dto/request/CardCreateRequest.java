package com.example.bankcards.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CardCreateRequest(
        @NotBlank(message = "Card number is required")
        @Pattern(regexp = "^[0-9 ]{16,23}$", message = "Card number must contain 16 digits")
        String number,

        @NotNull(message = "Owner id is required")
        UUID ownerId,

        @NotNull(message = "Expiration date is required")
        LocalDate expirationDate,

        @NotNull(message = "Balance is required")
        @DecimalMin(value = "0.00", message = "Balance cannot be negative")
        BigDecimal balance
) {
}
