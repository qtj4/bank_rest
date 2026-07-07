package com.example.bankcards.service;

import com.example.bankcards.dto.request.CardCreateRequest;
import com.example.bankcards.dto.request.CardUpdateRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.exception.AccessDeniedOperationException;
import com.example.bankcards.exception.BusinessErrorCode;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.exception.ConflictException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.mapper.CardMapper;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.spec.CardSpecifications;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardService {

    private static final Logger log = LoggerFactory.getLogger(CardService.class);

    private final CardRepository cardRepository;
    private final UserService userService;
    private final CurrentUserService currentUserService;
    private final CardCryptoService cardCryptoService;
    private final CardMapper cardMapper;
    private final MessageService messageService;

    public CardService(
            CardRepository cardRepository,
            UserService userService,
            CurrentUserService currentUserService,
            CardCryptoService cardCryptoService,
            CardMapper cardMapper,
            MessageService messageService
    ) {
        this.cardRepository = cardRepository;
        this.userService = userService;
        this.currentUserService = currentUserService;
        this.cardCryptoService = cardCryptoService;
        this.cardMapper = cardMapper;
        this.messageService = messageService;
    }

    @Transactional
    public CardResponse create(CardCreateRequest request) {
        if (request.getExpirationDate().isBefore(LocalDate.now())) {
            throw new BusinessException(
                    BusinessErrorCode.CARD_CREATE_EXPIRED,
                    messageService.get("business.card.create-expired")
            );
        }
        String normalizedNumber = cardCryptoService.normalize(request.getNumber());
        String hash = cardCryptoService.hash(normalizedNumber);
        if (cardRepository.existsByNumberHash(hash)) {
            throw new ConflictException(messageService.get("business.card.duplicate"));
        }
        User owner = userService.getEntity(request.getOwnerId());
        Card card = new Card();
        card.setEncryptedNumber(cardCryptoService.encrypt(normalizedNumber));
        card.setNumberHash(hash);
        card.setLastFourDigits(cardCryptoService.lastFourDigits(normalizedNumber));
        card.setOwner(owner);
        card.setExpirationDate(request.getExpirationDate());
        card.setStatus(CardStatus.ACTIVE);
        card.setBalance(scaleMoney(request.getBalance()));
        try {
            Card saved = cardRepository.save(card);
            log.info("card_created cardId={} ownerId={}", saved.getId(), owner.getId());
            return cardMapper.toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException(messageService.get("business.card.duplicate"));
        }
    }

    @Transactional(readOnly = true)
    public CardResponse getAdmin(UUID id) {
        return cardMapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public Page<CardResponse> listAll(CardStatus status, UUID ownerId, String lastFourDigits, Pageable pageable) {
        Specification<Card> specification = Specification.allOf(
                CardSpecifications.notDeleted(),
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
                CardSpecifications.notDeleted(),
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
        if (request.getExpirationDate() != null) {
            if (request.getExpirationDate().isBefore(LocalDate.now())) {
                throw new BusinessException(
                        BusinessErrorCode.CARD_EXPIRATION_IN_PAST,
                        messageService.get("business.card.expiration-past")
                );
            }
            card.setExpirationDate(request.getExpirationDate());
        }
        if (request.getBalance() != null) {
            card.setBalance(scaleMoney(request.getBalance()));
        }
        if (request.getStatus() != null) {
            if (request.getStatus() == CardStatus.ACTIVE && card.getExpirationDate().isBefore(LocalDate.now())) {
                throw new BusinessException(
                        BusinessErrorCode.CARD_EXPIRED,
                        messageService.get("business.card.activate-expired")
                );
            }
            card.setStatus(request.getStatus());
        }
        return cardMapper.toResponse(card);
    }

    @Transactional
    public CardResponse block(UUID id) {
        Card card = getEntity(id);
        card.setStatus(CardStatus.BLOCKED);
        card.setBlockRequested(false);
        card.setBlockRequestedAt(null);
        log.info("card_blocked cardId={}", card.getId());
        return cardMapper.toResponse(card);
    }

    @Transactional
    public CardResponse activate(UUID id) {
        Card card = getEntity(id);
        if (card.getExpirationDate().isBefore(LocalDate.now())) {
            throw new BusinessException(
                    BusinessErrorCode.CARD_EXPIRED,
                    messageService.get("business.card.activate-expired")
            );
        }
        card.setStatus(CardStatus.ACTIVE);
        card.setBlockRequested(false);
        card.setBlockRequestedAt(null);
        log.info("card_activated cardId={}", card.getId());
        return cardMapper.toResponse(card);
    }

    @Transactional
    public CardResponse requestBlock(UUID id) {
        Card card = getOwnCard(id);
        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new BusinessException(
                    BusinessErrorCode.CARD_ALREADY_BLOCKED,
                    messageService.get("business.card.already-blocked")
            );
        }
        card.setBlockRequested(true);
        card.setBlockRequestedAt(LocalDateTime.now());
        log.info("card_block_requested cardId={} userId={}", card.getId(), currentUserService.getCurrentUser().getId());
        return cardMapper.toResponse(card);
    }

    @Transactional
    public void delete(UUID id) {
        Card card = getEntity(id);
        card.setStatus(CardStatus.BLOCKED);
        UUID deletedBy = currentUserService.getCurrentUser().getId();
        card.markDeleted(deletedBy);
        log.info("card_soft_deleted cardId={} deletedBy={}", card.getId(), deletedBy);
    }

    Card getEntity(UUID id) {
        return cardRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException(messageService.get("business.card.not-found")));
    }

    private Card getOwnCard(UUID id) {
        User currentUser = currentUserService.getCurrentUser();
        Card card = cardRepository.findByIdAndOwnerIdAndDeletedAtIsNull(id, currentUser.getId())
                .orElseThrow(() -> new NotFoundException(messageService.get("business.card.not-found")));
        if (!card.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedOperationException(messageService.get("business.card.another-user"));
        }
        return card;
    }

    private BigDecimal scaleMoney(BigDecimal value) {
        if (value.stripTrailingZeros().scale() > 2) {
            throw new BusinessException(
                    BusinessErrorCode.INVALID_MONEY_SCALE,
                    messageService.get("business.money.invalid-scale")
            );
        }
        return value.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
