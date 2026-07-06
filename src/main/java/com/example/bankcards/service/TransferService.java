package com.example.bankcards.service;

import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.dto.response.TransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.exception.AccessDeniedOperationException;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {

    private final TransferRepository transferRepository;
    private final CardRepository cardRepository;
    private final CurrentUserService currentUserService;
    private final TransferMapper transferMapper;

    public TransferService(
            TransferRepository transferRepository,
            CardRepository cardRepository,
            CurrentUserService currentUserService,
            TransferMapper transferMapper
    ) {
        this.transferRepository = transferRepository;
        this.cardRepository = cardRepository;
        this.currentUserService = currentUserService;
        this.transferMapper = transferMapper;
    }

    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        if (request.fromCardId().equals(request.toCardId())) {
            throw new BusinessException("Source and destination cards must be different");
        }
        BigDecimal amount = scaleMoney(request.amount());
        if (amount.signum() <= 0) {
            throw new BusinessException("Transfer amount must be positive");
        }

        User currentUser = currentUserService.getCurrentUser();
        Map<UUID, Card> lockedCards = lockCards(request.fromCardId(), request.toCardId());
        Card fromCard = lockedCards.get(request.fromCardId());
        Card toCard = lockedCards.get(request.toCardId());
        if (fromCard == null || toCard == null) {
            throw new NotFoundException("Card not found");
        }

        assertOwnedByCurrentUser(fromCard, currentUser, "Source card belongs to another user");
        assertOwnedByCurrentUser(toCard, currentUser, "Destination card belongs to another user");
        assertUsable(fromCard, "Source card cannot be used for transfers");
        assertUsable(toCard, "Destination card cannot be used for transfers");
        if (fromCard.getBalance().compareTo(amount) < 0) {
            throw new BusinessException("Insufficient balance");
        }

        fromCard.setBalance(fromCard.getBalance().subtract(amount));
        toCard.setBalance(toCard.getBalance().add(amount));

        Transfer transfer = new Transfer();
        transfer.setFromCard(fromCard);
        transfer.setToCard(toCard);
        transfer.setAmount(amount);
        transfer.setDescription(request.description());
        return transferMapper.toResponse(transferRepository.save(transfer));
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
            throw new AccessDeniedOperationException("Transfer belongs to another user");
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
        return transferRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Transfer not found"));
    }

    private void assertOwnedByCurrentUser(Card card, User currentUser, String message) {
        if (!card.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedOperationException(message);
        }
    }

    private void assertUsable(Card card, String message) {
        if (card.getStatus() != CardStatus.ACTIVE || card.getExpirationDate().isBefore(LocalDate.now())) {
            throw new BusinessException(message);
        }
    }

    private void validateDateRange(LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException("Date range start must be before or equal to range end");
        }
    }

    private BigDecimal scaleMoney(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
