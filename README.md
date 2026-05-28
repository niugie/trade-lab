# trade-lab

[![CI](https://github.com/YOUR_USERNAME/trade-lab/actions/workflows/ci.yml/badge.svg)](https://github.com/YOUR_USERNAME/trade-lab/actions/workflows/ci.yml)

**Transaction domain lab** for backend portfolio — order state machine, inventory reserve/confirm/release, coupon freeze, and Outbox + RabbitMQ.

> Java 17 · Spring Boot 3 · MySQL · Redis · RabbitMQ · Flyway

## Highlights (for interviews)

| Topic | Implementation |
|-------|----------------|
| Order state machine | `PENDING_PAY` → `PAID` / `CLOSED` |
| Inventory | `available` / `reserved` ledger, optimistic SQL updates |
| Coupon | `AVAILABLE` → `FROZEN` → `USED`, release on close |
| Idempotency | `(user_id, idempotency_key)` unique; pay_no unique |
| Reliable messaging | Transactional **Outbox** → RabbitMQ |
| Timeout close | Scheduler scans expired `PENDING_PAY` orders |

## Architecture

```
Client → OrderController → OrderService
                              ├─ InventoryService (reserve)
                              ├─ CouponService (freeze)
                              ├─ PricingService
                              └─ OutboxService (same TX)
OutboxPublisher (scheduled) → RabbitMQ → OrderCloseConsumer (idempotent)
OrderExpireScheduler → closeOrder → release inventory & coupon
```

## Quick start

### 1. Start infrastructure

```bash
docker compose up -d
```

Services: MySQL `3306`, Redis `6379`, RabbitMQ `5672` (UI `15672`, user `trade` / `trade123`).

### 2. Run application

Requires **JDK 17+** (`java -version`).

```bash
mvn spring-boot:run
```

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html

### 3. Demo flow

```bash
# Create order (demo user 10001 has coupon id=1)
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":10001,"skuId":1,"quantity":1,"userCouponId":1,"idempotencyKey":"demo-001"}'

# Pay (replace ORDER_ID)
curl -X POST http://localhost:8080/api/v1/orders/ORDER_ID/pay \
  -H "Content-Type: application/json" \
  -d '{"payNo":"PAY-001"}'

# Check inventory
curl http://localhost:8080/api/v1/inventory/1
```

## API

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/orders` | Create order (reserve stock, optional coupon freeze) |
| GET | `/api/v1/orders/{id}` | Query order |
| POST | `/api/v1/orders/{id}/pay` | Pay (confirm stock, confirm coupon) |
| POST | `/api/v1/orders/{id}/close` | Close & release resources |
| GET | `/api/v1/inventory/{skuId}` | Query inventory ledger |

## Project structure

```
src/main/java/com/tradelab/
├── api/              # REST + DTO + exception handler
├── application/      # Use cases (order, inventory, coupon, outbox)
├── domain/           # Entities & enums
├── infrastructure/   # JPA, RabbitMQ, schedulers
└── common/           # Error codes, Snowflake ID
```

## Consistency notes

1. **Create order**: single transaction — reserve inventory, freeze coupon, insert order, append outbox.
2. **Pay**: confirm reserved stock (deduct `reserved`); coupon `FROZEN` → `USED`.
3. **Close / expire**: release `reserved` back to `available`; coupon back to `AVAILABLE`.
4. **Outbox**: at-least-once publish; consumer uses `message_consume_log` for idempotency.

## Tests

```bash
mvn test
```

Uses H2 in-memory (RabbitMQ auto-config disabled).

## Push to GitHub

```bash
# On GitHub: New repository → name: trade-lab → Public → do NOT add README
git remote add origin https://github.com/YOUR_USERNAME/trade-lab.git
git branch -M main
git push -u origin main
```

Replace `YOUR_USERNAME` and update the CI badge URL in this README.

## License

MIT
