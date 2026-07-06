# Bank Card Management System - Repo And Start Instructions

This file is for the project owner and for the person testing the submitted project.

The project is standalone. Do not describe it as part of any other system.

## For The Owner

Recommended repository name:

```bash
bank-card-management
```

Initialize Git:

```bash
git init
git branch -M main
git add .
git commit -m "Initial bank card management system"
git remote add origin <your-git-repository-url>
git push -u origin main
```

Before submitting, run:

```bash
mvn test
docker compose up --build
```

Then check:

```text
Health:     http://localhost:8080/actuator/health
Swagger UI: http://localhost:8080/swagger-ui/index.html
```

Stop:

```bash
docker compose down
```

Reset database:

```bash
docker compose down -v
```

## Required Cleanup

The starter project may contain temporary descriptive files such as:

- `README Controller.md`
- `README Service.md`
- `README Repository.md`
- similar README files inside source directories

Delete these temporary README files before final submission. Keep the root `README.md`.

## What To Submit

Submit only a public Git repository link.

The repository must contain:

- Source code.
- Root `README.md`.
- `Dockerfile`.
- `docker-compose.yml`.
- Liquibase migrations under `src/main/resources/db/migration`.
- `docs/openapi.yaml`.
- Swagger/OpenAPI setup.
- Unit tests for key business rules.
- No temporary source-directory README files.
- No committed production secrets.

Do not submit files manually outside Git.

## For The Tester

Prerequisites:

- Docker.
- Docker Compose.
- Free local port `8080`.
- Free local port `5432`, or adjust the PostgreSQL port mapping in `docker-compose.yml`.

Start:

```bash
git clone <repository-url>
cd bank-card-management
docker compose up --build
```

Open:

```text
Health:     http://localhost:8080/actuator/health
Swagger UI: http://localhost:8080/swagger-ui/index.html
OpenAPI:    docs/openapi.yaml
```

Use the local admin credentials documented in `README.md`.

Recommended manual test flow:

1. Login as admin and copy the JWT.
2. Create a user with role `USER`.
3. Create two cards for that user.
4. Verify the card numbers are returned only as masks, for example `**** **** **** 1234`.
5. Login as the user.
6. List own cards with pagination and filters.
7. Check balance for one own card.
8. Transfer money between the two own active cards.
9. Try to transfer more than available balance and expect `400`.
10. Try to access another user's card and expect `403` or `404`.
11. Request blocking for an own card.
12. Login as admin and block that card.
13. Verify transfer with the blocked card fails.
14. Activate the card as admin if it is not expired.
15. Verify validation errors include clear messages and field names.

Stop:

```bash
docker compose down
```

Remove containers and database volume:

```bash
docker compose down -v
```

## API Summary

Base URL:

```text
http://localhost:8080
```

Auth:

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

Useful query parameters:

```text
/api/v1/cards/my?page=0&size=10&status=ACTIVE&lastFourDigits=1234
/api/v1/cards/all?page=0&size=10&status=BLOCKED&ownerId=<uuid>
/api/v1/admin/users?page=0&size=10&search=ivan&role=USER&enabled=true
/api/v1/transfers/my?page=0&size=10&from=2026-07-01T00:00:00&to=2026-07-31T23:59:59
```

## Security Expectations

The tester should verify:

- Full card number is never returned.
- Card number is not stored in plain text.
- Passwords are BCrypt hashes.
- Requests without JWT return `401`.
- USER cannot call ADMIN endpoints.
- USER cannot see or use another user's cards.
- Transfers are rejected for blocked or expired cards.
- Transfers are rejected when balance is insufficient.
