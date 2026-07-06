package com.example.bankcards.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransferResponse(
        UUID id,
        UUID fromCardId,
        String fromMaskedNumber,
        UUID toCardId,
        String toMaskedNumber,
        BigDecimal amount,
        String description,
        LocalDateTime createdAt
) {
}
