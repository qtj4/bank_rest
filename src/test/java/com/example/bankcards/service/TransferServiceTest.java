package com.example.bankcards.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.enums.Role;
import com.example.bankcards.exception.AccessDeniedOperationException;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.mapper.TransferMapper;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private TransferRepository transferRepository;
    @Mock
    private CardRepository cardRepository;
    @Mock
    private CurrentUserService currentUserService;

    private TransferService transferService;
    private MessageService messageService;
    private User owner;
    private Card fromCard;
    private Card toCard;

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        CardCryptoService cardCryptoService = new CardCryptoService("unit-test-card-secret");
        cardCryptoService.init();
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultLocale(Locale.ENGLISH);
        messageSource.setFallbackToSystemLocale(false);
        messageService = new MessageService(messageSource);
        transferService = new TransferService(
                transferRepository,
                cardRepository,
                currentUserService,
                testTransferMapper(cardCryptoService),
                messageService
        );
        owner = user(UUID.randomUUID());
        fromCard = card(owner, new BigDecimal("100.00"), CardStatus.ACTIVE);
        toCard = card(owner, new BigDecimal("20.00"), CardStatus.ACTIVE);
    }

    @Test
    void transferBetweenOwnActiveCardsSucceedsAndUpdatesBalances() {
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(cardRepository.findAllByIdWithWriteLock(any())).thenReturn(List.of(fromCard, toCard));
        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        transferService.transfer(new TransferRequest(fromCard.getId(), toCard.getId(), new BigDecimal("30.00"), "top up"));

        assertThat(fromCard.getBalance()).isEqualByComparingTo("70.00");
        assertThat(toCard.getBalance()).isEqualByComparingTo("50.00");
    }

    @Test
    void transferToSameCardFails() {
        assertThatThrownBy(() -> transferService.transfer(
                new TransferRequest(fromCard.getId(), fromCard.getId(), BigDecimal.ONE, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("different");
    }

    @Test
    void transferFromAnotherUsersCardFails() {
        User other = user(UUID.randomUUID());
        fromCard.setOwner(other);
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(cardRepository.findAllByIdWithWriteLock(any())).thenReturn(List.of(fromCard, toCard));

        assertThatThrownBy(() -> transferService.transfer(
                new TransferRequest(fromCard.getId(), toCard.getId(), BigDecimal.ONE, null)))
                .isInstanceOf(AccessDeniedOperationException.class);
    }

    @Test
    void transferToAnotherUsersCardFails() {
        User other = user(UUID.randomUUID());
        toCard.setOwner(other);
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(cardRepository.findAllByIdWithWriteLock(any())).thenReturn(List.of(fromCard, toCard));

        assertThatThrownBy(() -> transferService.transfer(
                new TransferRequest(fromCard.getId(), toCard.getId(), BigDecimal.ONE, null)))
                .isInstanceOf(AccessDeniedOperationException.class);
    }

    @Test
    void transferWithBlockedCardFails() {
        fromCard.setStatus(CardStatus.BLOCKED);
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(cardRepository.findAllByIdWithWriteLock(any())).thenReturn(List.of(fromCard, toCard));

        assertThatThrownBy(() -> transferService.transfer(
                new TransferRequest(fromCard.getId(), toCard.getId(), BigDecimal.ONE, null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void transferWithExpiredCardFails() {
        fromCard.setExpirationDate(LocalDate.now().minusDays(1));
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(cardRepository.findAllByIdWithWriteLock(any())).thenReturn(List.of(fromCard, toCard));

        assertThatThrownBy(() -> transferService.transfer(
                new TransferRequest(fromCard.getId(), toCard.getId(), BigDecimal.ONE, null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void transferWithInsufficientBalanceFails() {
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(cardRepository.findAllByIdWithWriteLock(any())).thenReturn(List.of(fromCard, toCard));

        assertThatThrownBy(() -> transferService.transfer(
                new TransferRequest(fromCard.getId(), toCard.getId(), new BigDecimal("1000.00"), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient");
    }

    @Test
    void transferAmountMustBePositive() {
        assertThatThrownBy(() -> transferService.transfer(
                new TransferRequest(fromCard.getId(), toCard.getId(), BigDecimal.ZERO, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void dateRangeStartMustNotBeAfterEnd() {
        LocalDateTime from = LocalDateTime.now();
        LocalDateTime to = from.minusDays(1);

        assertThatThrownBy(() -> transferService.listMy(from, to, org.springframework.data.domain.Pageable.unpaged()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Date range");
    }

    private User user(UUID id) {
        User user = new User();
        user.setId(id);
        user.setUsername("user-" + id);
        user.setFullName("Test User");
        user.setRole(Role.USER);
        user.setEnabled(true);
        return user;
    }

    private Card card(User user, BigDecimal balance, CardStatus status) {
        Card card = new Card();
        card.setId(UUID.randomUUID());
        card.setOwner(user);
        card.setLastFourDigits("1234");
        card.setEncryptedNumber("encrypted");
        card.setNumberHash(UUID.randomUUID().toString());
        card.setExpirationDate(LocalDate.now().plusYears(1));
        card.setStatus(status);
        card.setBalance(balance);
        return card;
    }

    private TransferMapper testTransferMapper(CardCryptoService cardCryptoService) {
        CardCryptoService crypto = cardCryptoService;
        return new TransferMapper() {
            @Override
            public com.example.bankcards.dto.response.TransferResponse toResponse(Transfer transfer) {
                return com.example.bankcards.dto.response.TransferResponse.builder()
                        .id(transfer.getId())
                        .fromCardId(transfer.getFromCard().getId())
                        .fromMaskedNumber(crypto.mask(transfer.getFromCard().getLastFourDigits()))
                        .toCardId(transfer.getToCard().getId())
                        .toMaskedNumber(crypto.mask(transfer.getToCard().getLastFourDigits()))
                        .amount(transfer.getAmount())
                        .description(transfer.getDescription())
                        .createdAt(transfer.getCreatedAt())
                        .build();
            }
        };
    }
}
