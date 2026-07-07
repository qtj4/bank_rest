package com.example.bankcards.mapper;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.enums.CardStatus;
import java.time.LocalDate;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
public class CardMappingHelper {

    @Named("mask")
    public String mask(String lastFourDigits) {
        return "**** **** **** " + lastFourDigits;
    }

    @Named("effectiveStatus")
    public CardStatus effectiveStatus(Card card) {
        if (card.getStatus() == CardStatus.ACTIVE && card.getExpirationDate().isBefore(LocalDate.now())) {
            return CardStatus.EXPIRED;
        }
        return card.getStatus();
    }
}
