package com.example.bankcards.service;

import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.dto.response.TransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.exception.AccessDeniedOperationException;
import com.example.bankcards.exception.BusinessErrorCode;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.mapper.TransferMapper;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferRepository;
import com.example.bankcards.repository.spec.TransferSpecifications;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final TransferRepository transferRepository;
    private final CardRepository cardRepository;
    private final CurrentUserService currentUserService;
    private final TransferMapper transferMapper;
    private final MessageService messageService;

    public TransferService(
            TransferRepository transferRepository,
            CardRepository cardRepository,
            CurrentUserService currentUserService,
            TransferMapper transferMapper,
            MessageService messageService
    ) {
        this.transferRepository = transferRepository;
        this.cardRepository = cardRepository;
        this.currentUserService = currentUserService;
        this.transferMapper = transferMapper;
        this.messageService = messageService;
    }

    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        if (request.getFromCardId().equals(request.getToCardId())) {
            throw new BusinessException(
                    BusinessErrorCode.SAME_CARD_TRANSFER,
                    messageService.get("business.transfer.same-card")
            );
        }
        BigDecimal amount = scaleMoney(request.getAmount());
        if (amount.signum() <= 0) {
            throw new BusinessException(
                    BusinessErrorCode.INVALID_TRANSFER_AMOUNT,
                    messageService.get("business.transfer.positive-amount")
            );
        }

        User currentUser = currentUserService.getCurrentUser();
        Map<UUID, Card> lockedCards = lockCards(request.getFromCardId(), request.getToCardId());
        Card fromCard = lockedCards.get(request.getFromCardId());
        Card toCard = lockedCards.get(request.getToCardId());
        if (fromCard == null || toCard == null) {
            throw new NotFoundException(messageService.get("business.card.not-found"));
        }

        assertOwnedByCurrentUser(fromCard, currentUser, "business.transfer.source-another-user");
        assertOwnedByCurrentUser(toCard, currentUser, "business.transfer.destination-another-user");
        assertUsable(fromCard, "business.transfer.source-unusable");
        assertUsable(toCard, "business.transfer.destination-unusable");
        if (fromCard.getBalance().compareTo(amount) < 0) {
            throw new BusinessException(
                    BusinessErrorCode.INSUFFICIENT_FUNDS,
                    messageService.get("business.transfer.insufficient-balance")
            );
        }

        fromCard.setBalance(fromCard.getBalance().subtract(amount));
        toCard.setBalance(toCard.getBalance().add(amount));

        Transfer transfer = new Transfer();
        transfer.setFromCard(fromCard);
        transfer.setToCard(toCard);
        transfer.setAmount(amount);
        transfer.setDescription(request.getDescription());
        Transfer saved = transferRepository.save(transfer);
        log.info(
                "transfer_created transferId={} userId={} fromCardId={} toCardId={}",
                saved.getId(),
                currentUser.getId(),
                fromCard.getId(),
                toCard.getId()
        );
        return transferMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<TransferResponse> listMy(LocalDateTime from, LocalDateTime to, Pageable pageable) {
        validateDateRange(from, to);
        User currentUser = currentUserService.getCurrentUser();
        Specification<Transfer> specification = Specification.allOf(
                TransferSpecifications.participantUser(currentUser.getId()),
                TransferSpecifications.from(from),
                TransferSpecifications.to(to)
        );
        return transferRepository.findAll(specification, pageable).map(transferMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public TransferResponse getMy(UUID id) {
        User currentUser = currentUserService.getCurrentUser();
        Transfer transfer = getEntity(id);
        boolean participant = transfer.getFromCard().getOwner().getId().equals(currentUser.getId())
                || transfer.getToCard().getOwner().getId().equals(currentUser.getId());
        if (!participant) {
            throw new AccessDeniedOperationException(messageService.get("business.transfer.another-user"));
        }
        return transferMapper.toResponse(transfer);
    }

    @Transactional(readOnly = true)
    public Page<TransferResponse> listAll(UUID userId, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        validateDateRange(from, to);
        Specification<Transfer> specification = Specification.allOf(
                TransferSpecifications.participantUser(userId),
                TransferSpecifications.from(from),
                TransferSpecifications.to(to)
        );
        return transferRepository.findAll(specification, pageable).map(transferMapper::toResponse);
    }

    private Map<UUID, Card> lockCards(UUID firstId, UUID secondId) {
        List<UUID> orderedIds = List.of(firstId, secondId).stream()
                .sorted(Comparator.comparing(UUID::toString))
                .toList();
        List<Card> cards = cardRepository.findAllByIdWithWriteLock(orderedIds);
        return cards.stream().collect(Collectors.toMap(Card::getId, Function.identity()));
    }

    private Transfer getEntity(UUID id) {
        return transferRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException(messageService.get("business.transfer.not-found")));
    }

    private void assertOwnedByCurrentUser(Card card, User currentUser, String messageKey) {
        if (!card.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedOperationException(messageService.get(messageKey));
        }
    }

    private void assertUsable(Card card, String messageKey) {
        if (card.getStatus() != CardStatus.ACTIVE || card.getExpirationDate().isBefore(LocalDate.now())) {
            String code = card.getExpirationDate().isBefore(LocalDate.now())
                    ? BusinessErrorCode.CARD_EXPIRED
                    : BusinessErrorCode.CARD_NOT_ACTIVE;
            throw new BusinessException(code, messageService.get(messageKey));
        }
    }

    private void validateDateRange(LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(
                    BusinessErrorCode.INVALID_DATE_RANGE,
                    messageService.get("business.transfer.invalid-date-range")
            );
        }
    }

    private BigDecimal scaleMoney(BigDecimal value) {
        if (value.stripTrailingZeros().scale() > 2) {
            throw new BusinessException(
                    BusinessErrorCode.INVALID_MONEY_SCALE,
                    messageService.get("business.money.invalid-scale")
            );
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
