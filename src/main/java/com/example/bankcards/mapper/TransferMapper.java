package com.example.bankcards.mapper;

import com.example.bankcards.dto.response.TransferResponse;
import com.example.bankcards.entity.Transfer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = CardMappingHelper.class)
public interface TransferMapper {

    @Mapping(target = "fromCardId", source = "fromCard.id")
    @Mapping(target = "fromMaskedNumber", source = "fromCard.lastFourDigits", qualifiedByName = "mask")
    @Mapping(target = "toCardId", source = "toCard.id")
    @Mapping(target = "toMaskedNumber", source = "toCard.lastFourDigits", qualifiedByName = "mask")
    TransferResponse toResponse(Transfer transfer);
}
