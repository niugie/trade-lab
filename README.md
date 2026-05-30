# trade-lab

[![CI](https://github.com/NiuSir/trade-lab/actions/workflows/ci.yml/badge.svg)](https://github.com/NiuSir/trade-lab/actions/workflows/ci.yml)

Spring Boot 3 交易域示例：订单状态机、库存预占/确认/释放、优惠券冻结、幂等与 Outbox + RabbitMQ。

> Java 17 · Spring Boot 3 · MySQL · Redis · RabbitMQ · Flyway

## 功能

| 模块 | 说明 |
|------|------|
| 订单 | `PENDING_PAY` → `PAID` / `CLOSED` |
| 库存 | `available` / `reserved` 账本，乐观锁 SQL 更新 |
| 优惠券 | `AVAILABLE` → `FROZEN` → `USED`，关单释放 |
| 幂等 | `(user_id, idempotency_key)` 唯一；`pay_no` 唯一 |
| 消息 | 事务 Outbox → RabbitMQ |
| 超时关单 | 定时扫描过期 `PENDING_PAY` 订单 |

## 架构

```
Client → OrderController → OrderService
                              ├─ InventoryService (reserve)
                              ├─ CouponService (freeze)
                              ├─ PricingService
                              └─ OutboxService (same TX)
OutboxPublisher (scheduled) → RabbitMQ → OrderCloseConsumer (idempotent)
OrderExpireScheduler → closeOrder → release inventory & coupon
```

## 快速开始

### 本地模式（无需 Docker）

需要 JDK 17+，使用 H2 内存库（与 MySQL 相同的种子数据）。

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### 完整环境（WSL + Docker）

```bash
docker compose up -d
./scripts/build-wsl.sh
./scripts/run-wsl-jar.sh
```

项目在 `/mnt/d` 上时，请用 `./scripts/build-wsl.sh` 编译，避免 Windows 文件锁导致 `mvn package` 失败。

Windows + Docker Desktop：

```bash
docker compose up -d
mvn spring-boot:run
```

服务端口：MySQL `3307`，Redis `6379`，RabbitMQ `5672`（管理界面 `15672`，账号 `trade` / `trade123`）。

- 控制台：http://localhost:8080/
- Swagger：http://localhost:8080/swagger-ui/index.html
- 健康检查：http://localhost:8080/actuator/health

### 接口示例

```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":10001,"skuId":1,"quantity":1,"userCouponId":1,"idempotencyKey":"demo-001"}'

curl -X POST http://localhost:8080/api/v1/orders/ORDER_ID/pay \
  -H "Content-Type: application/json" \
  -d '{"payNo":"PAY-001"}'

curl http://localhost:8080/api/v1/inventory/1
```

## API

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/orders` | 创建订单 |
| GET | `/api/v1/orders/{id}` | 查询订单 |
| POST | `/api/v1/orders/{id}/pay` | 支付 |
| POST | `/api/v1/orders/{id}/close` | 关单 |
| GET | `/api/v1/inventory/{skuId}` | 查询库存 |
| GET | `/api/v1/demo/state` | 聚合状态 |
| POST | `/api/v1/demo/reset` | 重置测试数据 |

## 项目结构

```
src/main/java/com/tradelab/
├── api/              # REST、DTO、异常处理
├── application/      # 业务逻辑
├── domain/           # 实体与枚举
├── infrastructure/   # JPA、RabbitMQ、定时任务
└── common/           # 错误码、Snowflake ID
```

## 一致性

1. **下单**：同一事务内预占库存、冻结优惠券、写订单、写 Outbox。
2. **支付**：确认预占库存；优惠券 `FROZEN` → `USED`。
3. **关单/超时**：释放 `reserved` 回 `available`；优惠券恢复 `AVAILABLE`。
4. **Outbox**：至少一次投递；消费者通过 `message_consume_log` 幂等。

## 测试

```bash
mvn test
```

测试使用 H2 内存库，RabbitMQ 自动配置已禁用。

## License

MIT
