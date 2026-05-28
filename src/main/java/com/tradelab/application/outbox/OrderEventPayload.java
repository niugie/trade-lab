package com.tradelab.application.outbox;

public record OrderEventPayload(
        Long orderId,
        Long userId,
        Long skuId,
        Integer quantity,
        Long userCouponId
) {}
