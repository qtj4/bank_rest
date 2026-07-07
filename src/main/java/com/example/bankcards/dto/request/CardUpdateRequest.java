package com.example.bankcards.dto.request;

import com.example.bankcards.entity.enums.CardStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardUpdateRequest {

    private LocalDate expirationDate;

    private CardStatus status;

    @DecimalMin(value = "0.00", message = "{validation.balance.min}")
    @Digits(integer = 17, fraction = 2, message = "{validation.money.scale}")
    private BigDecimal balance;
}
