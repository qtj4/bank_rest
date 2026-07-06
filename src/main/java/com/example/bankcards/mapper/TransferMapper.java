package com.example.bankcards.mapper;

import com.example.bankcards.dto.response.TransferResponse;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.service.CardCryptoService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class TransferMapper {

    @Autowired
    protected CardCryptoService cardCryptoService;

    @Mapping(target = "fromCardId", source = "fromCard.id")
    @Mapping(target = "fromMaskedNumber", expression = "java(cardCryptoService.mask(transfer.getFromCard().getLastFourDigits()))")
    @Mapping(target = "toCardId", source = "toCard.id")
    @Mapping(target = "toMaskedNumber", expression = "java(cardCryptoService.mask(transfer.getToCard().getLastFourDigits()))")
    public abstract TransferResponse toResponse(Transfer transfer);
}
