# Document Service

Backend-сервис для управления документами. Java 21 + Spring Boot 3.3, PostgreSQL, Liquibase.

## Структура проекта

```
document-service/
├── document-api/        # основной Spring Boot сервис
├── document-generator/  # утилита массовой генерации документов
├── docker-compose.yml   # PostgreSQL
└── README.md
```

## Быстрый старт

### 1. Поднять PostgreSQL

```bash
docker compose up -d
```

PostgreSQL будет доступен на `localhost:5432`, БД `document_db`.

### 2. Запустить сервис

```bash
cd document-api
mvn spring-boot:run
```

Сервис запустится на `http://localhost:8080`.
Liquibase автоматически применит все миграции при старте.

### 3. Запустить утилиту-генератор

Параметры задаются через `document-generator/src/main/resources/application.yml` или через аргументы JVM:

```bash
cd document-generator
mvn spring-boot:run -Dspring-boot.run.arguments="--generator.count=500 --generator.service-url=http://localhost:8080"
```

Параметры генератора:
| Параметр | По умолчанию | Описание |
|---|---|---|
| `generator.count` | 100 | Количество документов для создания |
| `generator.service-url` | http://localhost:8080 | URL сервиса |
| `generator.initiator` | generator-tool | Инициатор создания |

---

## API

### Создать документ
```
POST /api/v1/documents
Content-Type: application/json

{
  "author": "Иванов И.И.",
  "title": "Тестовый документ",
  "initiator": "user1"
}
```

### Получить документ с историей
```
GET /api/v1/documents/{id}
```

### Пакетное получение документов
```
POST /api/v1/documents/batch-get?page=0&size=20&sortBy=id&sortDir=asc
Content-Type: application/json

[1, 2, 3, 4, 5]
```

### Отправить на согласование (DRAFT -> SUBMITTED)
```
POST /api/v1/documents/submit
Content-Type: application/json

{
  "ids": [1, 2, 3],
  "initiator": "user1",
  "comment": "Готово к рассмотрению"
}
```

### Утвердить (SUBMITTED -> APPROVED)
```
POST /api/v1/documents/approve
Content-Type: application/json

{
  "ids": [1, 2, 3],
  "initiator": "manager1",
  "comment": "Одобрено"
}
```

### Поиск документов
Фильтрация по дате **создания** (`created_at`).

```
GET /api/v1/documents/search?status=DRAFT&author=Иванов&from=2024-01-01T00:00:00Z&to=2024-12-31T23:59:59Z&page=0&size=20&sortBy=createdAt&sortDir=desc
```

### Тест конкурентного утверждения
```
POST /api/v1/documents/concurrent-approve
Content-Type: application/json

{
  "documentId": 42,
  "initiator": "tester",
  "threads": 10,
  "attempts": 20
}
```

---

## Формат ошибок

Все ошибки возвращаются в едином формате:
```json
{
  "code": "NOT_FOUND",
  "message": "Document not found: id=42"
}
```

Коды: `NOT_FOUND` (404), `CONFLICT` (409), `VALIDATION_ERROR` (400), `INTERNAL_ERROR` (500).

---

## Фоновые процессы

| Воркер | Интервал | Описание |
|---|---|---|
| `SubmitWorker` | `document.workers.submit-interval-ms` (10 сек) | Берёт пачку DRAFT документов и отправляет на согласование |
| `ApproveWorker` | `document.workers.approve-interval-ms` (15 сек) | Берёт пачку SUBMITTED документов и утверждает |

Размер пачки: `document.workers.batch-size` (100 по умолчанию).

### Как смотреть прогресс по логам

```
# Прогресс генератора
INFO  GeneratorRunner - Progress: 100/500 created (0 failed), elapsed=1234ms
INFO  GeneratorRunner - Generation complete: total=500, success=500, failed=0, totalTime=5678ms

# Ход SubmitWorker
INFO  SubmitWorker - SubmitWorker: processing 100 DRAFT documents, ids=[1..100]
INFO  SubmitWorker - SubmitWorker: done in 234ms — submitted=98, failed=2

# Ход ApproveWorker
INFO  ApproveWorker - ApproveWorker: processing 100 SUBMITTED documents, ids=[1..100]
INFO  ApproveWorker - ApproveWorker: done in 345ms — approved=100, failed=0
```

---

## Конфигурация

`document-api/src/main/resources/application.yml`:
```yaml
document:
  workers:
    batch-size: 100          # размер пачки для воркеров
    submit-interval-ms: 10000
    approve-interval-ms: 15000
```

---

## Сборка и тесты

```bash
# Сборка всего проекта
mvn clean package

# Запуск тестов (требует Docker для Testcontainers)
cd document-api
mvn test
```

Тесты используют Testcontainers — Docker должен быть запущен.

---

## Опциональные улучшения (масштабирование)

### Обработка 5000+ id в одном запросе
- Разбить список на чанки (например, 500 id) и обрабатывать параллельно через `CompletableFuture`.
- Использовать `saveAll` + `flush` вместо отдельных `save` на каждый документ.
- Батчевый SELECT с `WHERE id = ANY(?)` вместо N отдельных запросов.
- Включить `hibernate.jdbc.batch_size` и `order_updates=true`.
- Рассмотреть оптимистичную блокировку (`@Version`) вместо пессимистичной для снижения contention.

### Реестр утверждений как отдельная система

**Вариант 1 — Отдельная БД:**
Создать отдельный `DataSource` для реестра. Использовать паттерн Saga/Outbox:
документ сохраняет событие в таблицу `outbox` в одной транзакции, отдельный процесс переносит событие в БД реестра.
При сбое — идемпотентный повтор через outbox-поллинг.

**Вариант 2 — Отдельный HTTP-сервис:**
Выделить `approval-registry-service` с собственной БД. Вызывать через REST/gRPC.
Для надёжности — Transactional Outbox + message broker (Kafka/RabbitMQ):
после approve публиковать событие `DocumentApproved`, реестр-сервис консьюмит и записывает.
Rollback основного документа при неуспехе — через saga-компенсацию или двухфазный коммит.
