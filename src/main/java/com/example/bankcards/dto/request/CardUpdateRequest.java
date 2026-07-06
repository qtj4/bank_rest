package com.example.bankcards.dto.request;

import com.example.bankcards.entity.enums.CardStatus;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CardUpdateRequest(
        LocalDate expirationDate,

        CardStatus status,

        @DecimalMin(value = "0.00", message = "Balance cannot be negative")
        BigDecimal balance
) {
}
