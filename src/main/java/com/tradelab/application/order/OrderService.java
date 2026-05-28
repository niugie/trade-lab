package com.tradelab.application.order;

import com.tradelab.application.coupon.CouponService;
import com.tradelab.application.inventory.InventoryService;
import com.tradelab.application.outbox.OrderEventPayload;
import com.tradelab.application.outbox.OrderEventType;
import com.tradelab.application.outbox.OutboxService;
import com.tradelab.application.pricing.PricingService;
import com.tradelab.common.BusinessException;
import com.tradelab.common.ErrorCode;
import com.tradelab.common.SnowflakeIdGenerator;
import com.tradelab.domain.order.OrderStatus;
import com.tradelab.domain.order.TradeOrder;
import com.tradelab.domain.product.Sku;
import com.tradelab.infrastructure.persistence.SkuRepository;
import com.tradelab.infrastructure.persistence.TradeOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final String AGGREGATE_ORDER = "ORDER";

    private final TradeOrderRepository tradeOrderRepository;
    private final SkuRepository skuRepository;
    private final InventoryService inventoryService;
    private final CouponService couponService;
    private final PricingService pricingService;
    private final OutboxService outboxService;
    private final SnowflakeIdGenerator idGenerator;

    @Value("${trade.order.pay-timeout-minutes:15}")
    private int payTimeoutMinutes;

    @Transactional
    public TradeOrder createOrder(Long userId, Long skuId, int quantity, Long userCouponId, String idempotencyKey) {
        return tradeOrderRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                .orElseGet(() -> doCreateOrder(userId, skuId, quantity, userCouponId, idempotencyKey));
    }

    private TradeOrder doCreateOrder(Long userId, Long skuId, int quantity, Long userCouponId, String idempotencyKey) {
        if (quantity <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Quantity must be positive");
        }

        Sku sku = skuRepository.findById(skuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "SKU not found"));

        PricingService.PriceResult price = pricingService.calculate(userId, sku.getPriceCents(), quantity, userCouponId);

        inventoryService.reserve(skuId, quantity);

        long orderId = idGenerator.nextId();
        if (price.userCouponId() != null) {
            couponService.freeze(userId, price.userCouponId(), orderId);
        }
        return saveOrder(orderId, userId, sku, quantity, price, idempotencyKey);
    }

    private TradeOrder saveOrder(long orderId, Long userId, Sku sku, int quantity,
                                 PricingService.PriceResult price, String idempotencyKey) {
        Instant expireAt = Instant.now().plus(payTimeoutMinutes, ChronoUnit.MINUTES);

        TradeOrder order = TradeOrder.builder()
                .id(orderId)
                .userId(userId)
                .skuId(sku.getId())
                .quantity(quantity)
                .unitPrice(sku.getPriceCents())
                .totalAmount(price.totalAmount())
                .discountAmount(price.discountAmount())
                .payAmount(price.payAmount())
                .status(OrderStatus.PENDING_PAY)
                .userCouponId(price.userCouponId())
                .idempotencyKey(idempotencyKey)
                .expireAt(expireAt)
                .build();

        tradeOrderRepository.save(order);

        outboxService.append(AGGREGATE_ORDER, String.valueOf(orderId), OrderEventType.ORDER_CREATED,
                new OrderEventPayload(orderId, userId, sku.getId(), quantity, price.userCouponId()));

        return order;
    }

    @Transactional
    public TradeOrder pay(Long orderId, String payNo) {
        TradeOrder order = tradeOrderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Order not found"));

        if (!order.getStatus().canPay()) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATE);
        }

        if (order.getExpireAt().isBefore(Instant.now())) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATE, "Order expired");
        }

        tradeOrderRepository.findByPayNo(payNo).ifPresent(existing -> {
            if (!existing.getId().equals(orderId)) {
                throw new BusinessException(ErrorCode.DUPLICATE_REQUEST, "PayNo already used");
            }
        });

        if (order.getPayNo() != null && order.getPayNo().equals(payNo)) {
            return order;
        }

        inventoryService.confirm(order.getSkuId(), order.getQuantity());

        if (order.getUserCouponId() != null) {
            couponService.confirm(order.getUserCouponId(), order.getId());
        }

        order.setStatus(OrderStatus.PAID);
        order.setPayNo(payNo);
        order.setPaidAt(Instant.now());
        tradeOrderRepository.save(order);

        outboxService.append(AGGREGATE_ORDER, String.valueOf(orderId), OrderEventType.ORDER_PAID,
                new OrderEventPayload(orderId, order.getUserId(), order.getSkuId(), order.getQuantity(), order.getUserCouponId()));

        return order;
    }

    @Transactional
    public TradeOrder closeOrder(Long orderId) {
        TradeOrder order = tradeOrderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Order not found"));

        if (!order.getStatus().canClose()) {
            return order;
        }

        inventoryService.release(order.getSkuId(), order.getQuantity());

        if (order.getUserCouponId() != null) {
            couponService.release(order.getUserCouponId(), order.getId());
        }

        order.setStatus(OrderStatus.CLOSED);
        order.setClosedAt(Instant.now());
        tradeOrderRepository.save(order);

        return order;
    }
}
