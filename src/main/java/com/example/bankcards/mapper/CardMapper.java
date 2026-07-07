package com.example.bankcards.mapper;

import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.entity.Card;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = CardMappingHelper.class)
public interface CardMapper {

    @Mapping(target = "maskedNumber", source = "lastFourDigits", qualifiedByName = "mask")
    @Mapping(target = "status", source = "card", qualifiedByName = "effectiveStatus")
    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "ownerName", source = "owner.fullName")
    CardResponse toResponse(Card card);
}
