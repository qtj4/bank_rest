package com.example.bankcards.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {

    @NotNull(message = "{validation.from-card-id.required}")
    private UUID fromCardId;

    @NotNull(message = "{validation.to-card-id.required}")
    private UUID toCardId;

    @NotNull(message = "{validation.amount.required}")
    @DecimalMin(value = "0.01", message = "{validation.amount.positive}")
    @Digits(integer = 17, fraction = 2, message = "{validation.money.scale}")
    private BigDecimal amount;

    @Size(max = 255, message = "{validation.description.size}")
    private String description;
}
