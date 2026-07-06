package com.example.bankcards.dto.response;

import com.example.bankcards.entity.enums.CardStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record CardResponse(
        UUID id,
        String maskedNumber,
        UUID ownerId,
        String ownerName,
        LocalDate expirationDate,
        CardStatus status,
        BigDecimal balance,
        boolean blockRequested,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
