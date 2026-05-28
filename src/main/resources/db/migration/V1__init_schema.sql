-- SKU & inventory ledger (reserve / confirm / release)
CREATE TABLE sku (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    name          VARCHAR(128) NOT NULL,
    price_cents   BIGINT       NOT NULL COMMENT 'unit price in cents',
    created_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
);

CREATE TABLE inventory_ledger (
    sku_id        BIGINT PRIMARY KEY,
    available     INT          NOT NULL DEFAULT 0,
    reserved      INT          NOT NULL DEFAULT 0,
    version       BIGINT       NOT NULL DEFAULT 0,
    updated_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_inventory_sku FOREIGN KEY (sku_id) REFERENCES sku (id)
);

-- Coupon templates & user coupons
CREATE TABLE coupon_template (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(64)  NOT NULL,
    type            VARCHAR(16)  NOT NULL COMMENT 'FIXED or PERCENT',
    discount_value  BIGINT       NOT NULL COMMENT 'cents for FIXED, basis points for PERCENT e.g. 900=9折',
    min_amount      BIGINT       NOT NULL DEFAULT 0,
    total_stock     INT          NOT NULL,
    claimed_count   INT          NOT NULL DEFAULT 0,
    valid_from      DATETIME(3)  NOT NULL,
    valid_to        DATETIME(3)  NOT NULL,
    created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
);

CREATE TABLE user_coupon (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    template_id     BIGINT       NOT NULL,
    status          VARCHAR(16)  NOT NULL DEFAULT 'AVAILABLE' COMMENT 'AVAILABLE,FROZEN,USED',
    frozen_order_id BIGINT       NULL,
    version         BIGINT       NOT NULL DEFAULT 0,
    claimed_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    used_at         DATETIME(3)  NULL,
    INDEX idx_user_coupon_user (user_id),
    INDEX idx_user_coupon_status (user_id, status),
    CONSTRAINT fk_user_coupon_template FOREIGN KEY (template_id) REFERENCES coupon_template (id)
);

-- Orders
CREATE TABLE trade_order (
    id              BIGINT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    sku_id          BIGINT       NOT NULL,
    quantity        INT          NOT NULL,
    unit_price      BIGINT       NOT NULL,
    total_amount    BIGINT       NOT NULL,
    discount_amount BIGINT       NOT NULL DEFAULT 0,
    pay_amount      BIGINT       NOT NULL,
    status          VARCHAR(24)  NOT NULL,
    user_coupon_id  BIGINT       NULL,
    idempotency_key VARCHAR(64)  NOT NULL,
    pay_no          VARCHAR(64)  NULL,
    expire_at       DATETIME(3)  NOT NULL,
    paid_at         DATETIME(3)  NULL,
    closed_at       DATETIME(3)  NULL,
    created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_order_idempotency (user_id, idempotency_key),
    UNIQUE KEY uk_order_pay_no (pay_no),
    INDEX idx_order_status_expire (status, expire_at)
);

-- Outbox for reliable messaging
CREATE TABLE outbox_message (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    aggregate_type  VARCHAR(32)  NOT NULL,
    aggregate_id    VARCHAR(64)  NOT NULL,
    event_type      VARCHAR(64)  NOT NULL,
    payload         TEXT         NOT NULL,
    status          VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    retry_count     INT          NOT NULL DEFAULT 0,
    next_retry_at   DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    sent_at         DATETIME(3)  NULL,
    INDEX idx_outbox_pending (status, next_retry_at)
);

-- Message consumption idempotency
CREATE TABLE message_consume_log (
    message_id      VARCHAR(128) PRIMARY KEY,
    consumer        VARCHAR(64)  NOT NULL,
    consumed_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
);

-- Seed demo data
INSERT INTO sku (id, name, price_cents) VALUES (1, 'Flash Sale T-Shirt', 9900);
INSERT INTO inventory_ledger (sku_id, available, reserved, version) VALUES (1, 1000, 0, 0);

INSERT INTO coupon_template (id, name, type, discount_value, min_amount, total_stock, valid_from, valid_to)
VALUES (1, 'New User 10 Off', 'FIXED', 1000, 5000, 10000,
        '2020-01-01 00:00:00.000', '2030-12-31 23:59:59.000');

INSERT INTO user_coupon (user_id, template_id, status) VALUES (10001, 1, 'AVAILABLE');
