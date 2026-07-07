package com.example.bankcards;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.bankcards.dto.request.CardCreateRequest;
import com.example.bankcards.dto.request.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.Role;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.exception.ConflictException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.TransferService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers(disabledWithoutDocker = true)
class BankCardsIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("security.jwt.secret", () -> "integration-test-jwt-secret-at-least-32-characters");
        registry.add("card.crypto.secret", () -> "integration-test-card-secret");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CardRepository cardRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private CardService cardService;
    @Autowired
    private TransferService transferService;

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
        SecurityContextHolder.clearContext();
    }

    @Test
    void liquibaseCreatesSchemaAndSeedsAdmin() {
        Integer adminCount = jdbcTemplate.queryForObject(
                "select count(*) from app_user where username = 'admin'",
                Integer.class
        );

        assertThat(adminCount).isEqualTo(1);
    }

    @Test
    void duplicateCardNumberIsRejectedAgainstPostgresConstraintPath() {
        User owner = createUser("duplicate-owner");
        String number = uniqueCardNumber("411111111111");

        cardService.create(new CardCreateRequest(number, owner.getId(), LocalDate.now().plusYears(1), BigDecimal.TEN));

        assertThatThrownBy(() -> cardService.create(new CardCreateRequest(
                number,
                owner.getId(),
                LocalDate.now().plusYears(1),
                BigDecimal.ONE
        ))).isInstanceOf(ConflictException.class);
    }

    @Test
    void userCannotAccessAnotherUsersCardThroughServiceBoundary() {
        User owner = createUser("card-owner");
        User anotherUser = createUser("another-user");
        UUID cardId = cardService.create(new CardCreateRequest(
                uniqueCardNumber("422222222222"),
                owner.getId(),
                LocalDate.now().plusYears(1),
                BigDecimal.TEN
        )).getId();
        authenticateAs(anotherUser);

        assertThatThrownBy(() -> cardService.getMy(cardId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void concurrentTransfersCannotOverspendSourceCard() throws Exception {
        User owner = createUser("concurrent-owner");
        UUID fromCardId = cardService.create(new CardCreateRequest(
                uniqueCardNumber("433333333333"),
                owner.getId(),
                LocalDate.now().plusYears(1),
                new BigDecimal("100.00")
        )).getId();
        UUID toCardId = cardService.create(new CardCreateRequest(
                uniqueCardNumber("444444444444"),
                owner.getId(),
                LocalDate.now().plusYears(1),
                BigDecimal.ZERO
        )).getId();

        Callable<Boolean> transferTask = () -> {
            authenticateAs(owner);
            try {
                transferService.transfer(new TransferRequest(fromCardId, toCardId, new BigDecimal("80.00"), "race"));
                return true;
            } catch (BusinessException exception) {
                return false;
            } finally {
                SecurityContextHolder.clearContext();
            }
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Boolean> results = executor.invokeAll(List.of(transferTask, transferTask)).stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .toList();

            assertThat(results).containsExactlyInAnyOrder(true, false);
        } finally {
            executor.shutdownNow();
        }

        Card fromCard = cardRepository.findByIdAndDeletedAtIsNull(fromCardId).orElseThrow();
        Card toCard = cardRepository.findByIdAndDeletedAtIsNull(toCardId).orElseThrow();
        assertThat(fromCard.getBalance()).isEqualByComparingTo("20.00");
        assertThat(toCard.getBalance()).isEqualByComparingTo("80.00");
    }

    private User createUser(String prefix) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        User user = new User();
        user.setUsername(prefix + "-" + suffix);
        user.setPassword(passwordEncoder.encode("Password123"));
        user.setFullName("Integration User");
        user.setRole(Role.USER);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    private String uniqueCardNumber(String twelveDigitPrefix) {
        String suffix = String.format("%04d", Math.abs(UUID.randomUUID().hashCode()) % 10_000);
        return twelveDigitPrefix + suffix;
    }

    private void authenticateAs(User user) {
        var principal = org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }
}
