package com.example.bankcards.service;

import com.example.bankcards.dto.request.CardCreateRequest;
import com.example.bankcards.dto.request.CardUpdateRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.exception.AccessDeniedOperationException;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.exception.ConflictException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.mapper.CardMapper;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferRepository;
import com.example.bankcards.repository.spec.CardSpecifications;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardService {

    private final CardRepository cardRepository;
    private final TransferRepository transferRepository;
    private final UserService userService;
    private final CurrentUserService currentUserService;
    private final CardCryptoService cardCryptoService;
    private final CardMapper cardMapper;

    public CardService(
            CardRepository cardRepository,
            TransferRepository transferRepository,
            UserService userService,
            CurrentUserService currentUserService,
            CardCryptoService cardCryptoService,
            CardMapper cardMapper
    ) {
        this.cardRepository = cardRepository;
        this.transferRepository = transferRepository;
        this.userService = userService;
        this.currentUserService = currentUserService;
        this.cardCryptoService = cardCryptoService;
        this.cardMapper = cardMapper;
    }

    @Transactional
    public CardResponse create(CardCreateRequest request) {
        if (request.expirationDate().isBefore(LocalDate.now())) {
            throw new BusinessException("Expired cards cannot be created");
        }
        String normalizedNumber = cardCryptoService.normalize(request.number());
        String hash = cardCryptoService.hash(normalizedNumber);
        if (cardRepository.existsByNumberHash(hash)) {
            throw new ConflictException("Card number already exists");
        }
        User owner = userService.getEntity(request.ownerId());
        Card card = new Card();
        card.setEncryptedNumber(cardCryptoService.encrypt(normalizedNumber));
        card.setNumberHash(hash);
        card.setLastFourDigits(cardCryptoService.lastFourDigits(normalizedNumber));
        card.setOwner(owner);
        card.setExpirationDate(request.expirationDate());
        card.setStatus(CardStatus.ACTIVE);
        card.setBalance(scaleMoney(request.balance()));
        try {
            return cardMapper.toResponse(cardRepository.save(card));
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Card number already exists");
        }
    }

    @Transactional(readOnly = true)
    public CardResponse getAdmin(UUID id) {
        return cardMapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public Page<CardResponse> listAll(CardStatus status, UUID ownerId, String lastFourDigits, Pageable pageable) {
        Specification<Card> specification = Specification.allOf(
                CardSpecifications.status(status),
                CardSpecifications.ownerId(ownerId),
                CardSpecifications.lastFourDigits(lastFourDigits)
        );
        return cardRepository.findAll(specification, pageable).map(cardMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<CardResponse> listMy(CardStatus status, String lastFourDigits, Pageable pageable) {
        User currentUser = currentUserService.getCurrentUser();
        Specification<Card> specification = Specification.allOf(
                CardSpecifications.ownerId(currentUser.getId()),
                CardSpecifications.status(status),
                CardSpecifications.lastFourDigits(lastFourDigits)
        );
        return cardRepository.findAll(specification, pageable).map(cardMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CardResponse getMy(UUID id) {
        return cardMapper.toResponse(getOwnCard(id));
    }

    @Transactional(readOnly = true)
    public BigDecimal getOwnBalance(UUID id) {
        return getOwnCard(id).getBalance();
    }

    @Transactional
    public CardResponse update(UUID id, CardUpdateRequest request) {
        Card card = getEntity(id);
        if (request.expirationDate() != null) {
            if (request.expirationDate().isBefore(LocalDate.now())) {
                throw new BusinessException("Expiration date cannot be in the past");
            }
            card.setExpirationDate(request.expirationDate());
        }
        if (request.balance() != null) {
            card.setBalance(scaleMoney(request.balance()));
        }
        if (request.status() != null) {
            if (request.status() == CardStatus.ACTIVE && card.getExpirationDate().isBefore(LocalDate.now())) {
                throw new BusinessException("Expired card cannot be activated");
            }
            card.setStatus(request.status());
        }
        return cardMapper.toResponse(card);
    }

    @Transactional
    public CardResponse block(UUID id) {
        Card card = getEntity(id);
        card.setStatus(CardStatus.BLOCKED);
        card.setBlockRequested(false);
        card.setBlockRequestedAt(null);
        return cardMapper.toResponse(card);
    }

    @Transactional
    public CardResponse activate(UUID id) {
        Card card = getEntity(id);
        if (card.getExpirationDate().isBefore(LocalDate.now())) {
            throw new BusinessException("Expired card cannot be activated");
        }
        card.setStatus(CardStatus.ACTIVE);
        card.setBlockRequested(false);
        card.setBlockRequestedAt(null);
        return cardMapper.toResponse(card);
    }

    @Transactional
    public CardResponse requestBlock(UUID id) {
        Card card = getOwnCard(id);
        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new BusinessException("Card is already blocked");
        }
        card.setBlockRequested(true);
        card.setBlockRequestedAt(LocalDateTime.now());
        return cardMapper.toResponse(card);
    }

    @Transactional
    public void delete(UUID id) {
        Card card = getEntity(id);
        if (transferRepository.existsByFromCardIdOrToCardId(id, id)) {
            card.setStatus(CardStatus.BLOCKED);
            return;
        }
        cardRepository.delete(card);
    }

    Card getEntity(UUID id) {
        return cardRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Card not found"));
    }

    private Card getOwnCard(UUID id) {
        User currentUser = currentUserService.getCurrentUser();
        Card card = cardRepository.findByIdAndOwnerId(id, currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Card not found"));
        if (!card.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedOperationException("Card belongs to another user");
        }
        return card;
    }

    private BigDecimal scaleMoney(BigDecimal value) {
        return value.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
