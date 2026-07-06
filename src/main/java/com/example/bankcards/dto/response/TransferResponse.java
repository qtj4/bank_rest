package com.example.bankcards.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponse {
    private UUID id;
    private UUID fromCardId;
    private String fromMaskedNumber;
    private UUID toCardId;
    private String toMaskedNumber;
    private BigDecimal amount;
    private String description;
    private LocalDateTime createdAt;
}
