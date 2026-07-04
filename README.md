# Система управления банковскими картами

Backend REST API (Java 21, Spring Boot, PostgreSQL) для управления банковскими картами, пользователями и переводами.

## Требования

- Java 21+
- Maven 3.9+
- Docker и Docker Compose

## Запуск

### 1. Переменные окружения

```bash
cp .env.example .env
```

В `.env` задайте `JWT_SECRET` (не короче 32 символов).

### 2. PostgreSQL

```bash
docker compose up -d postgres
```

### 3. Приложение (локально)

```bash
set -a && source .env && set +a
mvn spring-boot:run
```

### 4. Приложение (Docker Compose)

```bash
docker compose up --build
```

Приложение: http://localhost:8080  
Swagger UI: http://localhost:8080/swagger-ui/index.html  
OpenAPI: [docs/openapi.yaml](docs/openapi.yaml)

Авторизация: заголовок `Authorization: Bearer <JWT>` (токен из `/api/auth/login`).

## Postman

Для удобства тестирования API добавлена коллекция: [docs/postman_collection.json](docs/postman_collection.json).

Импорт: Postman → **Import** → выбрать файл. После **Login** JWT сохраняется в переменную `token` автоматически.

### Первый ADMIN

Регистрация через API создаёт пользователя с ролью `USER`. Роль ADMIN назначается в БД:

```bash
docker exec -it bankcards-postgres psql -U bankcards -d bankcards
```

```sql
INSERT INTO user_roles (user_id, role_id)
SELECT 1, id FROM roles WHERE name = 'ADMIN';
```

(`1` — id пользователя из `SELECT id, username FROM users;`. После назначения роли выполните Login заново.)

## Тесты

```bash
mvn test
```
