package com.example.bankcards.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardCreateRequest {

    @NotBlank(message = "{validation.card-number.required}")
    @Pattern(regexp = "^[0-9 ]{16,23}$", message = "{validation.card-number.invalid}")
    private String number;

    @NotNull(message = "{validation.owner-id.required}")
    private UUID ownerId;

    @NotNull(message = "{validation.expiration-date.required}")
    private LocalDate expirationDate;

    @NotNull(message = "{validation.balance.required}")
    @DecimalMin(value = "0.00", message = "{validation.balance.min}")
    @Digits(integer = 17, fraction = 2, message = "{validation.money.scale}")
    private BigDecimal balance;
}
