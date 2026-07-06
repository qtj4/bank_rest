# Bank Card Management

Standalone Spring Boot backend for managing users, bank cards, card blocking, balances, and transfers between a user's own cards.

## Technology Stack

- Java 17
- Spring Boot 3
- Spring Web, Security, Validation, Data JPA, Actuator
- JWT authentication
- PostgreSQL
- Liquibase
- Swagger/OpenAPI
- Lombok
- MapStruct
- Maven
- Docker Compose
- JUnit 5, Mockito, AssertJ

## Run With Docker

```bash
docker compose up --build
```

Health check:

```text
http://localhost:8080/actuator/health
```

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI file:

```text
docs/openapi.yaml
```

Postman collection:

```text
postman/bank-card-management.postman_collection.json
```

Stop:

```bash
docker compose down
```

Reset database:

```bash
docker compose down -v
```

## Local Admin

The local Liquibase seed creates one admin account:

```text
username: admin
password: admin123
```

These credentials are for local testing only.

## JWT Usage

1. Call `POST /api/v1/auth/login`.
2. Copy the `token` value from the response.
3. Send protected requests with:

```text
Authorization: Bearer <token>
```

## Environment Variables

- `APP_PORT` - application port, default `8080`
- `DB_URL` - JDBC URL, default `jdbc:postgresql://localhost:5432/bank_card_db`
- `DB_USER` - database user, default `postgres`
- `DB_PASSWORD` - database password, default `postgres`
- `JWT_SECRET` - JWT signing secret, at least 32 characters
- `JWT_EXPIRATION_MS` - JWT lifetime in milliseconds, default `3600000`
- `CARD_CRYPTO_SECRET` - card encryption secret, at least 16 characters
- `LIQUIBASE_ENABLED` - enables migrations, default `true`
- `SHOW_SQL` - enables SQL logging, default `false`

Override `JWT_SECRET` and `CARD_CRYPTO_SECRET` outside local development.

## API Summary

Authentication:

```text
POST /api/v1/auth/register
POST /api/v1/auth/login
```

Admin users:

```text
POST   /api/v1/admin/users
GET    /api/v1/admin/users/{id}
GET    /api/v1/admin/users
PUT    /api/v1/admin/users/{id}
DELETE /api/v1/admin/users/{id}
```

Cards:

```text
POST   /api/v1/cards
GET    /api/v1/cards/{id}
GET    /api/v1/cards/all
GET    /api/v1/cards/my
GET    /api/v1/cards/my/{id}
GET    /api/v1/cards/{id}/balance
PUT    /api/v1/cards/{id}
PATCH  /api/v1/cards/{id}/block
PATCH  /api/v1/cards/{id}/activate
POST   /api/v1/cards/{id}/block-request
DELETE /api/v1/cards/{id}
```

Transfers:

```text
POST /api/v1/transfers
GET  /api/v1/transfers/my
GET  /api/v1/transfers/{id}
GET  /api/v1/admin/transfers
```

Useful filters:

```text
/api/v1/cards/my?page=0&size=10&status=ACTIVE&lastFourDigits=1234
/api/v1/cards/all?page=0&size=10&status=BLOCKED&ownerId=<uuid>
/api/v1/admin/users?page=0&size=10&search=ivan&role=USER&enabled=true
/api/v1/transfers/my?page=0&size=10&from=2026-07-01T00:00:00&to=2026-07-31T23:59:59
```

## Business Rules

- Card numbers must contain exactly 16 digits.
- Full card numbers are never returned by the API.
- Card numbers are stored as AES-GCM ciphertext plus SHA-256 hash for uniqueness checks.
- Passwords are stored as BCrypt hashes.
- Endpoint access is checked with permission constants and `@PreAuthorize("hasPermission(...)")`, backed by role-to-permission mapping.
- Admins create, update, block, activate, soft-delete, and list all cards.
- Users can list only their own cards, view own balances, request blocking, and transfer between own cards.
- Transfers require two different active, non-expired cards owned by the authenticated user.
- Transfers are atomic and lock the two card rows in deterministic UUID order.
- Insufficient balance, blocked cards, expired cards, invalid date ranges, and duplicate card numbers return business errors.
- User deletion disables the account and marks `deletedAt/deletedBy`.
- Card deletion blocks the card and marks `deletedAt/deletedBy`.

## Localization

Validation and business error messages are localized through Spring message bundles:

```text
src/main/resources/messages.properties
src/main/resources/messages_ru.properties
```

Use the `Accept-Language` header to select messages:

```text
Accept-Language: en
Accept-Language: ru
```

## Code Quality Notes

- Lombok is used for DTOs and JPA boilerplate, but JPA entities use `@Getter/@Setter` instead of `@Data` to avoid unsafe generated `equals/hashCode/toString` on lazy relations.
- MapStruct is used for response mapping so DTO shape is explicit and mapper code is generated at compile time.
- Permission constants plus a lightweight `PermissionEvaluator` keep authorization rules near controller methods without introducing a permission table/role-management subsystem for this assignment.
- Soft-delete audit fields are placed in `BaseEntity`; normal user/card reads filter out deleted rows while preserving history.
- Transfers use deterministic pessimistic locking by UUID to reduce race-condition and deadlock risk during balance updates.
- User-facing errors and validation messages are centralized and localized, which keeps controllers clean and improves API polish.

## Postman

Import:

```text
postman/bank-card-management.postman_collection.json
```

Run the collection from top to bottom after `docker compose up --build`.

The collection:

- logs in as the seeded admin and stores `adminToken`;
- creates a unique user and stores `userId`;
- logs in as that user and stores `userToken`;
- creates two cards and stores `card1Id` and `card2Id`;
- performs successful card, balance, transfer, and block flows;
- verifies negative cases for missing auth, forbidden access, validation errors, duplicate cards, expired cards, blocked cards, insufficient balance, and invalid date ranges.

## Tests

Run:

```bash
mvn test
```

The unit tests cover authentication hashing/login behavior, card masking and ownership rules, status transitions, duplicate card numbers, transfer validation, locked balance updates, and date-range validation.
