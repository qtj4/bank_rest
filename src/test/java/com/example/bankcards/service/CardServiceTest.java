package com.example.bankcards.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.bankcards.dto.request.CardCreateRequest;
import com.example.bankcards.dto.response.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.CardStatus;
import com.example.bankcards.entity.enums.Role;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.exception.ConflictException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.mapper.CardMapper;
import com.example.bankcards.repository.CardRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private CardRepository cardRepository;
    @Mock
    private UserService userService;
    @Mock
    private CurrentUserService currentUserService;

    private CardCryptoService cardCryptoService;
    private CardService cardService;
    private MessageService messageService;
    private User owner;

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        cardCryptoService = new CardCryptoService("unit-test-card-secret");
        cardCryptoService.init();
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("messages");
        messageSource.setDefaultLocale(Locale.ENGLISH);
        messageSource.setFallbackToSystemLocale(false);
        messageService = new MessageService(messageSource);
        cardService = new CardService(
                cardRepository,
                userService,
                currentUserService,
                cardCryptoService,
                testCardMapper(),
                messageService
        );
        owner = user(UUID.randomUUID());
    }

    @Test
    void adminCreatesCardSuccessfullyWithMaskedResponse() {
        CardCreateRequest request = new CardCreateRequest(
                "1234 5678 9012 3456",
                owner.getId(),
                LocalDate.now().plusYears(1),
                new BigDecimal("100.00")
        );
        when(userService.getEntity(owner.getId())).thenReturn(owner);
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CardResponse response = cardService.create(request);

        assertThat(response.getMaskedNumber()).isEqualTo("**** **** **** 3456");
        assertThat(response.getBalance()).isEqualByComparingTo("100.00");
        ArgumentCaptor<Card> captor = ArgumentCaptor.forClass(Card.class);
        verify(cardRepository).save(captor.capture());
        assertThat(captor.getValue().getEncryptedNumber()).doesNotContain("1234567890123456");
        assertThat(captor.getValue().getNumberHash()).hasSize(64);
    }

    @Test
    void duplicateCardNumberIsRejectedByHash() {
        CardCreateRequest request = new CardCreateRequest(
                "1234567890123456",
                owner.getId(),
                LocalDate.now().plusYears(1),
                BigDecimal.ZERO
        );
        when(cardRepository.existsByNumberHash(cardCryptoService.hash("1234567890123456"))).thenReturn(true);

        assertThatThrownBy(() -> cardService.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void userCanSeeOwnCard() {
        Card card = card(owner, CardStatus.ACTIVE);
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(cardRepository.findByIdAndOwnerIdAndDeletedAtIsNull(card.getId(), owner.getId())).thenReturn(Optional.of(card));

        CardResponse response = cardService.getMy(card.getId());

        assertThat(response.getId()).isEqualTo(card.getId());
    }

    @Test
    void userCannotSeeAnotherUsersCard() {
        UUID cardId = UUID.randomUUID();
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(cardRepository.findByIdAndOwnerIdAndDeletedAtIsNull(cardId, owner.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.getMy(cardId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void userCanRequestBlockForOwnCard() {
        Card card = card(owner, CardStatus.ACTIVE);
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(cardRepository.findByIdAndOwnerIdAndDeletedAtIsNull(card.getId(), owner.getId())).thenReturn(Optional.of(card));

        CardResponse response = cardService.requestBlock(card.getId());

        assertThat(response.isBlockRequested()).isTrue();
        assertThat(card.getBlockRequestedAt()).isNotNull();
    }

    @Test
    void adminBlocksCard() {
        Card card = card(owner, CardStatus.ACTIVE);
        card.setBlockRequested(true);
        when(cardRepository.findByIdAndDeletedAtIsNull(card.getId())).thenReturn(Optional.of(card));

        CardResponse response = cardService.block(card.getId());

        assertThat(response.getStatus()).isEqualTo(CardStatus.BLOCKED);
        assertThat(response.isBlockRequested()).isFalse();
    }

    @Test
    void adminActivatesBlockedNonExpiredCard() {
        Card card = card(owner, CardStatus.BLOCKED);
        when(cardRepository.findByIdAndDeletedAtIsNull(card.getId())).thenReturn(Optional.of(card));

        CardResponse response = cardService.activate(card.getId());

        assertThat(response.getStatus()).isEqualTo(CardStatus.ACTIVE);
    }

    @Test
    void expiredCardCannotBeActivated() {
        Card card = card(owner, CardStatus.BLOCKED);
        card.setExpirationDate(LocalDate.now().minusDays(1));
        when(cardRepository.findByIdAndDeletedAtIsNull(card.getId())).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> cardService.activate(card.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Expired");
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

    private Card card(User user, CardStatus status) {
        Card card = new Card();
        card.setId(UUID.randomUUID());
        card.setOwner(user);
        card.setLastFourDigits("3456");
        card.setEncryptedNumber("encrypted");
        card.setNumberHash("hash");
        card.setExpirationDate(LocalDate.now().plusYears(1));
        card.setStatus(status);
        card.setBalance(new BigDecimal("100.00"));
        return card;
    }

    private CardMapper testCardMapper() {
        return new CardMapper() {
            @Override
            public CardResponse toResponse(Card card) {
                return CardResponse.builder()
                        .id(card.getId())
                        .maskedNumber(CardServiceTest.this.cardCryptoService.mask(card.getLastFourDigits()))
                        .ownerId(card.getOwner().getId())
                        .ownerName(card.getOwner().getFullName())
                        .expirationDate(card.getExpirationDate())
                        .status(card.getStatus())
                        .balance(card.getBalance())
                        .blockRequested(card.isBlockRequested())
                        .createdAt(card.getCreatedAt())
                        .updatedAt(card.getUpdatedAt())
                        .deletedAt(card.getDeletedAt())
                        .deletedBy(card.getDeletedBy())
                        .build();
            }
        };
    }
}
