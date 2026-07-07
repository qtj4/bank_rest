package com.example.bankcards.controller;

import com.example.bankcards.dto.request.CardCreateRequest;
import com.example.bankcards.dto.request.CardUpdateRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cards")
@Tag(name = "Cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a card for a user")
    @PreAuthorize("hasPermission(null, @permission.CARD_CREATE)")
    public CardResponse create(@Valid @RequestBody CardCreateRequest request) {
        return cardService.create(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get any card as admin")
    @PreAuthorize("hasPermission(null, @permission.CARD_VIEW_ALL)")
    public CardResponse getAdmin(@PathVariable UUID id) {
        return cardService.getAdmin(id);
    }

    @GetMapping("/all")
    @Operation(summary = "List all cards as admin")
    @PreAuthorize("hasPermission(null, @permission.CARD_VIEW_ALL)")
    public Page<CardResponse> listAll(
            @RequestParam(required = false) CardStatus status,
            @RequestParam(required = false) UUID ownerId,
            @RequestParam(required = false) String lastFourDigits,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return cardService.listAll(status, ownerId, lastFourDigits, pageable);
    }

    @GetMapping("/my")
    @Operation(summary = "List current user's cards")
    @PreAuthorize("hasPermission(null, @permission.CARD_VIEW_OWN)")
    public Page<CardResponse> listMy(
            @RequestParam(required = false) CardStatus status,
            @RequestParam(required = false) String lastFourDigits,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return cardService.listMy(status, lastFourDigits, pageable);
    }

    @GetMapping("/my/{id}")
    @Operation(summary = "Get current user's card")
    @PreAuthorize("hasPermission(null, @permission.CARD_VIEW_OWN)")
    public CardResponse getMy(@PathVariable UUID id) {
        return cardService.getMy(id);
    }

    @GetMapping("/{id}/balance")
    @Operation(summary = "Get current user's card balance")
    @PreAuthorize("hasPermission(null, @permission.CARD_VIEW_OWN)")
    public Map<String, BigDecimal> balance(@PathVariable UUID id) {
        return Map.of("balance", cardService.getOwnBalance(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a card")
    @PreAuthorize("hasPermission(null, @permission.CARD_UPDATE)")
    public CardResponse update(@PathVariable UUID id, @Valid @RequestBody CardUpdateRequest request) {
        return cardService.update(id, request);
    }

    @PatchMapping("/{id}/block")
    @Operation(summary = "Block a card")
    @PreAuthorize("hasPermission(null, @permission.CARD_BLOCK)")
    public CardResponse block(@PathVariable UUID id) {
        return cardService.block(id);
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activate a card")
    @PreAuthorize("hasPermission(null, @permission.CARD_ACTIVATE)")
    public CardResponse activate(@PathVariable UUID id) {
        return cardService.activate(id);
    }

    @PostMapping("/{id}/block-request")
    @Operation(summary = "Request blocking for current user's card")
    @PreAuthorize("hasPermission(null, @permission.CARD_BLOCK_REQUEST)")
    public CardResponse requestBlock(@PathVariable UUID id) {
        return cardService.requestBlock(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a card, or block it when transfer history exists")
    @PreAuthorize("hasPermission(null, @permission.CARD_DELETE)")
    public void delete(@PathVariable UUID id) {
        cardService.delete(id);
    }
}
