package com.example.bankcards.mapper;

import com.example.bankcards.dto.response.TransferResponse;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.service.CardCryptoService;
import org.springframework.stereotype.Component;

@Component
public class TransferMapper {

    private final CardCryptoService cardCryptoService;

    public TransferMapper(CardCryptoService cardCryptoService) {
        this.cardCryptoService = cardCryptoService;
    }

    public TransferResponse toResponse(Transfer transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getFromCard().getId(),
                cardCryptoService.mask(transfer.getFromCard().getLastFourDigits()),
                transfer.getToCard().getId(),
                cardCryptoService.mask(transfer.getToCard().getLastFourDigits()),
                transfer.getAmount(),
                transfer.getDescription(),
                transfer.getCreatedAt()
        );
    }
}
