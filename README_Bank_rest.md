# Система управления банковскими картами

Backend REST API на Java 21 + Spring Boot 4 для управления банковскими картами, пользователями и переводами.

## Требования

- Java 21+
- Maven 3.9+
- Docker и Docker Compose (для PostgreSQL)

## Быстрый старт

### 1. Переменные окружения

```bash
cp .env.example .env
```

Заполните `.env`. Минимум нужен `JWT_SECRET` (не короче 32 символов).

### 2. Запуск PostgreSQL

```bash
docker compose up -d postgres
```

### 3. Запуск приложения

```bash
set -a && source .env && set +a
mvn spring-boot:run
```

Приложение: http://localhost:8080  
Swagger UI: http://localhost:8080/swagger-ui/index.html

### 4. Полный запуск в Docker

```bash
docker compose up --build
```

## Первый ADMIN

Регистрация через API создаёт пользователя с ролью `USER`. Первого администратора назначьте в БД:

```sql
INSERT INTO user_roles (user_id, role_id)
SELECT 1, id FROM roles WHERE name = 'ADMIN';
```

## Основные API

| Метод | URL | Доступ |
|-------|-----|--------|
| POST | `/api/auth/register` | публичный |
| POST | `/api/auth/login` | публичный |
| GET | `/api/cards` | ADMIN |
| GET | `/api/cards/my` | авторизованный |
| POST | `/api/cards` | ADMIN |
| POST | `/api/transfers` | USER / ADMIN |
| GET | `/api/users` | ADMIN |

Авторизация: заголовок `Authorization: Bearer <JWT>`.

## Тесты

```bash
mvn test
```

## Стек

Java 21, Spring Boot 4, Spring Security, JWT, Spring Data JPA, PostgreSQL, Liquibase, Docker, OpenAPI (springdoc).
