package com.example.bankcards.dto.response;

import com.example.bankcards.entity.enums.CardStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
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
public class CardResponse {
    private UUID id;
    private String maskedNumber;
    private UUID ownerId;
    private String ownerName;
    private LocalDate expirationDate;
    private CardStatus status;
    private BigDecimal balance;
    private boolean blockRequested;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private UUID deletedBy;
}
