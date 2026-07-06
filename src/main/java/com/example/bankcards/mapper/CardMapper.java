package com.example.bankcards.mapper;

import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.service.CardCryptoService;
import org.springframework.stereotype.Component;

@Component
public class CardMapper {

    private final CardCryptoService cardCryptoService;

    public CardMapper(CardCryptoService cardCryptoService) {
        this.cardCryptoService = cardCryptoService;
    }

    public CardResponse toResponse(Card card) {
        return new CardResponse(
                card.getId(),
                cardCryptoService.mask(card.getLastFourDigits()),
                card.getOwner().getId(),
                card.getOwner().getFullName(),
                card.getExpirationDate(),
                card.getStatus(),
                card.getBalance(),
                card.isBlockRequested(),
                card.getCreatedAt(),
                card.getUpdatedAt()
        );
    }
}
