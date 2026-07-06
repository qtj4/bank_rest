# Bank Card Management System - Agent Prompt

Use this prompt for an agentic AI that must complete a standalone backend project for managing bank cards.

Do not reference any existing company, platform, monorepo, or previous project. This is a standalone assignment.

## Starting Context

The project already contains a starter directory structure with temporary descriptive files such as:

- `README Controller.md`
- `README Service.md`
- `README Repository.md`
- similar README files in prepared directories

Add implementations only into the matching directories.

After the implementation is finished, delete all temporary descriptive README files from source directories so they do not get into the final submission or build artifact. Keep the root `README.md`.

## Goal

Develop a Java Spring Boot backend application for bank card management.

The system must support:

- Authentication and authorization with Spring Security and JWT.
- Roles: `ADMIN` and `USER`.
- Creating and managing bank cards.
- Viewing cards with search, filtering, and pagination.
- Transfers between a user's own cards.
- Validation and centralized error handling.
- Encrypted card number storage and masked card number output.
- Liquibase migrations.
- Swagger/OpenAPI documentation.
- Docker Compose dev environment.
- Unit tests for key business logic.

## Technology Stack

Use:

- Java 17 or newer. Prefer Java 21 if the starter project already uses it.
- Maven.
- Spring Boot 3.x.
- Spring Web.
- Spring Security.
- Spring Data JPA.
- Spring Validation.
- PostgreSQL. MySQL is allowed by the assignment, but PostgreSQL is preferred.
- Liquibase.
- Docker and Docker Compose.
- JWT.
- Swagger/OpenAPI.
- JUnit 5, Mockito, AssertJ.
- Testcontainers only where integration tests are useful.
- Lombok and MapStruct are allowed if already configured in the starter project. If not configured, add them only if they reduce boilerplate without confusing the project.

## Architecture Principles

Use a clean, simple layered architecture:

- `controller` handles HTTP only.
- `service` contains business rules and transactions.
- `repository` contains persistence access.
- `entity` contains JPA entities only.
- `dto/request` and `dto/response` are API contracts.
- `mapper` maps between entities and DTOs.
- `security` contains JWT and Spring Security code.
- `exception` and `handler` contain application exceptions and centralized error handling.
- `config` contains framework configuration.

Do not overengineer:

- Do not create generic CRUD service interfaces unless the starter project already requires them.
- Do not return JPA entities from controllers.
- Do not put business logic in controllers.
- Do not create extra abstractions for only one implementation.
- Do not introduce Kafka, Redis, external identity providers, or message queues.
- Do not implement microservices.

## Recommended Package Structure

Adapt package names to the starter project. A good standalone shape is:

```text
src/main/java/com/example/cardmanagement/
  CardManagementApplication.java
  config/
    OpenApiConfig.java
    JpaAuditConfig.java
    PasswordConfig.java
  controller/
    AuthController.java
    UserController.java
    CardController.java
    TransferController.java
  dto/
    request/
      LoginRequest.java
      RegisterRequest.java
      UserCreateRequest.java
      UserUpdateRequest.java
      CardCreateRequest.java
      CardUpdateRequest.java
      TransferRequest.java
    response/
      AuthResponse.java
      UserResponse.java
      CardResponse.java
      TransferResponse.java
      ErrorResponse.java
  entity/
    BaseEntity.java
    User.java
    Card.java
    Transfer.java
    enums/
      Role.java
      CardStatus.java
  exception/
    AccessDeniedOperationException.java
    BusinessException.java
    NotFoundException.java
  handler/
    GlobalExceptionHandler.java
  mapper/
    UserMapper.java
    CardMapper.java
    TransferMapper.java
  repository/
    UserRepository.java
    CardRepository.java
    TransferRepository.java
    spec/
      CardSpecifications.java
      UserSpecifications.java
      TransferSpecifications.java
  security/
    JwtAuthenticationFilter.java
    JwtService.java
    SecurityConfig.java
    UserDetailsServiceImpl.java
  service/
    AuthService.java
    UserService.java
    CardService.java
    TransferService.java
    CardCryptoService.java
```

OpenAPI file:

```text
docs/openapi.yaml
```

Liquibase path required by the assignment:

```text
src/main/resources/db/migration
```

Use a master changelog and SQL migrations inside that folder.

## Domain Model

### BaseEntity

Use a small audit base class:

```java
@MappedSuperclass
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
```

Enable auditing:

```java
@Configuration
@EnableJpaAuditing
public class JpaAuditConfig {
}
```

### User

Fields:

- `id`
- `username`
- `password`
- `fullName`
- `role`
- `enabled`
- `createdAt`
- `updatedAt`

Rules:

- Username must be unique.
- Password must be stored as BCrypt hash.
- Only admins can manage users.
- Normal users can only operate on their own cards.

Entity outline:

```java
@Entity
@Table(name = "app_user")
@Getter
@Setter
public class User extends BaseEntity {

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private Role role;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;
}
```

### Card

Required card attributes:

- Card number: stored encrypted, displayed masked as `**** **** **** 1234`.
- Owner.
- Expiration date.
- Status: active, blocked, expired.
- Balance.

Recommended fields:

- `id`
- `encryptedNumber`
- `numberHash`
- `lastFourDigits`
- `owner`
- `expirationDate`
- `status`
- `balance`
- `blockRequested`
- `blockRequestedAt`
- `createdAt`
- `updatedAt`

Use `blockRequested` for user block requests so `CardStatus` stays aligned with the assignment's three statuses.

```java
public enum CardStatus {
    ACTIVE,
    BLOCKED,
    EXPIRED
}
```

```java
@Entity
@Table(
        name = "card",
        indexes = {
                @Index(name = "card_owner_id_index", columnList = "owner_id"),
                @Index(name = "card_status_index", columnList = "status"),
                @Index(name = "card_last_four_digits_index", columnList = "last_four_digits")
        }
)
@Getter
@Setter
public class Card extends BaseEntity {

    @Column(name = "encrypted_number", nullable = false, columnDefinition = "text")
    private String encryptedNumber;

    @Column(name = "number_hash", nullable = false, unique = true)
    private String numberHash;

    @Column(name = "last_four_digits", nullable = false, length = 4)
    private String lastFourDigits;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "expiration_date", nullable = false)
    private LocalDate expirationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private CardStatus status = CardStatus.ACTIVE;

    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "block_requested", nullable = false)
    private boolean blockRequested = false;

    @Column(name = "block_requested_at")
    private LocalDateTime blockRequestedAt;
}
```

Store expiration as `LocalDate` to keep JPA mapping simple. Treat the card as valid through that date. If the API accepts only month/year, convert it to the last day of that month in the service and document the behavior in README.

### Transfer

Fields:

- `id`
- `fromCard`
- `toCard`
- `amount`
- `createdAt`

Optional but useful:

- `description`

Rules:

- User can transfer only between their own cards.
- Source and destination cards must be different.
- Both cards must be `ACTIVE`.
- Expired cards cannot be used.
- Amount must be positive.
- Source card must have enough balance.
- Transfer must be atomic in a single transaction.

```java
@Entity
@Table(name = "card_transfer")
@Getter
@Setter
public class Transfer extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_card_id", nullable = false)
    private Card fromCard;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_card_id", nullable = false)
    private Card toCard;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "description")
    private String description;
}
```

## Security

Implement Spring Security with JWT.

Requirements:

- `POST /api/v1/auth/register` creates a `USER` account, unless the starter already seeds users.
- `POST /api/v1/auth/login` returns JWT.
- Passwords are hashed with BCrypt.
- JWT contains username and role.
- Stateless session management.
- Admin endpoints require `ADMIN`.
- User endpoints require authenticated user.

Recommended access rules:

```text
POST /api/v1/auth/**                 permitAll
GET  /swagger-ui/**                  permitAll
GET  /v3/api-docs/**                 permitAll
GET  /actuator/health                permitAll
/api/v1/admin/**                     ADMIN
POST /api/v1/cards                   ADMIN
PUT  /api/v1/cards/{id}              ADMIN
DELETE /api/v1/cards/{id}            ADMIN
PATCH /api/v1/cards/{id}/block       ADMIN
PATCH /api/v1/cards/{id}/activate    ADMIN
GET /api/v1/cards/all                ADMIN
GET /api/v1/cards/my                 USER or ADMIN
POST /api/v1/cards/{id}/block-request USER or ADMIN
POST /api/v1/transfers               USER or ADMIN
```

Use method-level checks in services too. Do not rely only on URL security.

## Card Number Encryption And Masking

Never store card number in plain text.

Store:

- `encryptedNumber`: AES-GCM encrypted card number.
- `numberHash`: SHA-256 hash of normalized card number for uniqueness checks.
- `lastFourDigits`: last four digits for masking/search display.

Mask format:

```text
**** **** **** 1234
```

Rules:

- Accept card number only in create request.
- Validate card number as 16 digits unless the assignment starter says otherwise.
- Normalize by removing spaces.
- Do not return full card number in any response.
- Do not log raw card number.
- Encryption key comes from environment variable, for example `CARD_CRYPTO_SECRET`.
- If key is missing, fail startup with a clear message.

`CardResponse` should include:

- `id`
- `maskedNumber`
- `ownerId`
- `ownerName`
- `expirationDate`
- `status`
- `balance`
- `blockRequested`
- `createdAt`
- `updatedAt`

## API Endpoints

### Auth

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
```

### Admin User Management

```text
POST   /api/v1/admin/users
GET    /api/v1/admin/users/{id}
GET    /api/v1/admin/users?page=0&size=10&search=ivan&role=USER&enabled=true
PUT    /api/v1/admin/users/{id}
DELETE /api/v1/admin/users/{id}
```

Deletion can be physical delete or disabling the user. Prefer disabling if the user has cards or transfers.

### Cards

Admin:

```text
POST   /api/v1/cards
GET    /api/v1/cards/{id}
GET    /api/v1/cards/all?page=0&size=10&status=ACTIVE&ownerId=<uuid>&lastFourDigits=1234
PUT    /api/v1/cards/{id}
PATCH  /api/v1/cards/{id}/block
PATCH  /api/v1/cards/{id}/activate
DELETE /api/v1/cards/{id}
```

User:

```text
GET  /api/v1/cards/my?page=0&size=10&status=ACTIVE&lastFourDigits=1234
GET  /api/v1/cards/my/{id}
POST /api/v1/cards/{id}/block-request
GET  /api/v1/cards/{id}/balance
```

### Transfers

```text
POST /api/v1/transfers
GET  /api/v1/transfers/my?page=0&size=10&from=2026-07-01T00:00:00&to=2026-07-31T23:59:59
GET  /api/v1/transfers/{id}
```

Users can see only their own transfers. Admin may have:

```text
GET /api/v1/admin/transfers?page=0&size=10&userId=<uuid>
```

## DTO Validation

Examples:

```java
@Data
public class CardCreateRequest {

    @NotBlank(message = "{card.number.required}")
    @Pattern(regexp = "\\d{16}", message = "{card.number.invalid}")
    private String number;

    @NotNull(message = "{card.owner-id.required}")
    private UUID ownerId;

    @NotNull(message = "{card.expiration-date.required}")
    private LocalDate expirationDate;

    @NotNull(message = "{card.balance.required}")
    @DecimalMin(value = "0.00", message = "{card.balance.min}")
    private BigDecimal balance;
}
```

```java
@Data
public class TransferRequest {

    @NotNull(message = "{transfer.from-card-id.required}")
    private UUID fromCardId;

    @NotNull(message = "{transfer.to-card-id.required}")
    private UUID toCardId;

    @NotNull(message = "{transfer.amount.required}")
    @DecimalMin(value = "0.01", message = "{transfer.amount.positive}")
    private BigDecimal amount;

    @Size(max = 255, message = "{transfer.description.size}")
    private String description;
}
```

## Service Business Logic

### CardService

Admin actions:

- Create card for a user.
- Block card.
- Activate card.
- Delete card.
- See all cards.

User actions:

- See only own cards.
- Request card blocking.
- See balance of own card.

Rules:

- Only admin can create cards.
- Card number must be unique by hash.
- Expired card cannot be activated.
- Blocked card cannot be used in transfer.
- User cannot see another user's card.
- User cannot request block for another user's card.
- If expiration date is in the past, save status as `EXPIRED` or reject the create request. Prefer rejecting create request for expired cards.

### TransferService

Use `@Transactional`.

Important concurrency rule:

- Lock the involved cards when transferring money. Use repository methods with `@Lock(LockModeType.PESSIMISTIC_WRITE)` or a carefully designed update query.
- Always lock cards in a deterministic order, for example by UUID string, to avoid deadlocks.

Transfer algorithm:

1. Load authenticated user.
2. Load source and destination cards with write lock.
3. Verify both cards belong to current user.
4. Verify cards are different.
5. Verify both statuses are `ACTIVE`.
6. Verify expiration date is not in the past.
7. Verify amount is positive.
8. Verify source balance is enough.
9. Subtract from source.
10. Add to destination.
11. Save `Transfer`.
12. Return masked response.

## Error Handling

Use centralized `@RestControllerAdvice`.

Error response:

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private String error;
    private Integer status;
    private String message;
    private Map<String, String> fieldErrors;
    private LocalDateTime timestamp;
}
```

Handle:

- `NotFoundException` -> `404 NOT_FOUND`
- `BusinessException` -> `400 BUSINESS_RULE_VIOLATION`
- `AccessDeniedException` or custom access exception -> `403 FORBIDDEN`
- `MethodArgumentNotValidException` -> `400 VALIDATION_ERROR` with field errors
- `ConstraintViolationException` -> `400 VALIDATION_ERROR`
- `DataIntegrityViolationException` -> `409 DATA_INTEGRITY_ERROR`
- fallback `Exception` -> `500 INTERNAL_SERVER_ERROR`

Never expose stack traces or sensitive values in API responses.

## Liquibase

The assignment requires:

```text
src/main/resources/db/migration
```

Use this structure:

```text
src/main/resources/db/migration/
  db.changelog-master.yaml
  sql/
    001_create_users_cards_transfers.sql
    002_insert_default_admin.sql
```

`db.changelog-master.yaml`:

```yaml
databaseChangeLog:
  - includeAll:
      path: db/migration/sql
```

Application config:

```yaml
spring:
  liquibase:
    enabled: ${LIQUIBASE_ENABLED:true}
    change-log: classpath:db/migration/db.changelog-master.yaml
```

Initial SQL should create:

- `app_user`
- `card`
- `card_transfer`
- indexes
- unique constraints
- foreign keys
- check constraints for roles and statuses

Recommended SQL details:

```sql
create table app_user
(
    id uuid not null constraint app_user_pk primary key,
    username varchar(255) not null,
    password varchar(255) not null,
    full_name varchar(255) not null,
    role varchar(32) not null,
    enabled boolean not null default true,
    created_at timestamp without time zone not null,
    updated_at timestamp without time zone not null,
    constraint app_user_role_check check (role in ('ADMIN', 'USER'))
);

create unique index app_user_username_uindex on app_user (username);

create table card
(
    id uuid not null constraint card_pk primary key,
    encrypted_number text not null,
    number_hash varchar(255) not null,
    last_four_digits varchar(4) not null,
    owner_id uuid not null,
    expiration_date date not null,
    status varchar(32) not null,
    balance numeric(19, 2) not null default 0,
    block_requested boolean not null default false,
    block_requested_at timestamp without time zone,
    created_at timestamp without time zone not null,
    updated_at timestamp without time zone not null,
    constraint card_owner_fk foreign key (owner_id) references app_user (id),
    constraint card_status_check check (status in ('ACTIVE', 'BLOCKED', 'EXPIRED')),
    constraint card_balance_non_negative_check check (balance >= 0),
    constraint card_last_four_digits_check check (last_four_digits ~ '^[0-9]{4}$')
);

create unique index card_number_hash_uindex on card (number_hash);
create index card_owner_id_index on card (owner_id);
create index card_status_index on card (status);
create index card_last_four_digits_index on card (last_four_digits);

create table card_transfer
(
    id uuid not null constraint card_transfer_pk primary key,
    from_card_id uuid not null,
    to_card_id uuid not null,
    amount numeric(19, 2) not null,
    description varchar(255),
    created_at timestamp without time zone not null,
    updated_at timestamp without time zone not null,
    constraint card_transfer_from_card_fk foreign key (from_card_id) references card (id),
    constraint card_transfer_to_card_fk foreign key (to_card_id) references card (id),
    constraint card_transfer_amount_positive_check check (amount > 0),
    constraint card_transfer_different_cards_check check (from_card_id <> to_card_id)
);

create index card_transfer_from_card_id_index on card_transfer (from_card_id);
create index card_transfer_to_card_id_index on card_transfer (to_card_id);
create index card_transfer_created_at_index on card_transfer (created_at);
```

Seed one default admin user for local testing. Store only a BCrypt hash, not a plain password. Document the local credentials in README.

## Application Configuration

Use `application.yml`:

```yaml
server:
  port: ${APP_PORT:8080}

spring:
  application:
    name: bank-card-management
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/bank_card_db}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:postgres}
  jpa:
    open-in-view: false
    show-sql: ${SHOW_SQL:false}
  liquibase:
    enabled: ${LIQUIBASE_ENABLED:true}
    change-log: classpath:db/migration/db.changelog-master.yaml
  jackson:
    default-property-inclusion: non_null

security:
  jwt:
    secret: ${JWT_SECRET:change-this-secret-in-production-change-this-secret}
    expiration-ms: ${JWT_EXPIRATION_MS:3600000}

card:
  crypto:
    secret: ${CARD_CRYPTO_SECRET:change-this-32-byte-secret-value}

springdoc:
  api-docs:
    enabled: true
  swagger-ui:
    enabled: true
```

For production-like behavior, do not use default JWT/encryption secrets. README must explain how to override them.

## Swagger And OpenAPI

Provide:

```text
docs/openapi.yaml
```

Also enable Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

Controllers should have:

- `@Tag`
- `@Operation`
- useful request/response schemas through DTOs
- security scheme documented as Bearer JWT

## Docker

Create `Dockerfile`:

```dockerfile
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

Create `docker-compose.yml`:

```yaml
services:
  postgres:
    image: postgres:16-alpine
    container_name: bank-card-postgres
    environment:
      POSTGRES_DB: bank_card_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d bank_card_db"]
      interval: 5s
      timeout: 5s
      retries: 10

  app:
    build: .
    container_name: bank-card-management-app
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/bank_card_db
      DB_USER: postgres
      DB_PASSWORD: postgres
      JWT_SECRET: local-development-jwt-secret-change-me-local-development
      CARD_CRYPTO_SECRET: local-development-card-secret-32b
      APP_PORT: 8080
    ports:
      - "8080:8080"

volumes:
  postgres_data:
```

If the encryption service requires an exact key size, make the compose secret match it and document the rule.

## Tests

Unit tests are required for key business logic.

Minimum `CardServiceTest`:

- Admin creates card successfully.
- Duplicate card number is rejected by hash.
- Card response returns masked number only.
- User can see own card.
- User cannot see another user's card.
- User can request block for own card.
- User cannot request block for another user's card.
- Admin blocks card.
- Admin activates blocked non-expired card.
- Expired card cannot be activated.

Minimum `TransferServiceTest`:

- Transfer between own active cards succeeds.
- Transfer to same card fails.
- Transfer from another user's card fails.
- Transfer to another user's card fails.
- Transfer with blocked card fails.
- Transfer with expired card fails.
- Transfer with insufficient balance fails.
- Transfer amount must be positive.
- Balances are updated correctly.
- Transfer record is saved.

Minimum `AuthServiceTest`:

- Register hashes password.
- Login returns JWT for valid credentials.
- Login fails for invalid password.

Controller tests:

- Missing auth returns 401.
- USER cannot call admin endpoints.
- Validation error returns `fieldErrors`.

## README

Root `README.md` must include:

- Project description.
- Technology stack.
- Required environment: Docker, Docker Compose, optional JDK/Maven.
- How to run:

```bash
docker compose up --build
```

- How to stop:

```bash
docker compose down
```

- How to reset DB:

```bash
docker compose down -v
```

- Swagger URL.
- OpenAPI file location: `docs/openapi.yaml`.
- Default local admin credentials.
- JWT usage instructions.
- Environment variables:
  - `DB_URL`
  - `DB_USER`
  - `DB_PASSWORD`
  - `JWT_SECRET`
  - `JWT_EXPIRATION_MS`
  - `CARD_CRYPTO_SECRET`
  - `APP_PORT`
- API endpoint summary.
- Business rules.
- Security notes:
  - card numbers encrypted
  - full card numbers never returned
  - passwords hashed
  - role restrictions
- Test command:

```bash
mvn test
```

## Final Cleanup

Before final delivery:

- Delete temporary directory README files from starter structure.
- Keep only the root `README.md`.
- Ensure no plain card numbers are present in logs, responses, tests, seed data, or README examples except clearly fake examples.
- Ensure no production secrets are committed.

## Acceptance Checklist

Verify:

- `mvn test` passes.
- `docker compose up --build` starts app and database.
- Liquibase creates tables and seed admin.
- Swagger UI opens.
- `docs/openapi.yaml` exists.
- Register/login works.
- ADMIN can create, block, activate, delete cards.
- ADMIN can manage users.
- ADMIN can see all cards.
- USER can see only own cards with search and pagination.
- USER can request block for own card.
- USER can transfer only between own active cards.
- Card numbers are encrypted at rest.
- API responses show only masked card numbers.
- Validation errors are clear and centralized.
- Role violations return 403.
- Missing or invalid JWT returns 401.
