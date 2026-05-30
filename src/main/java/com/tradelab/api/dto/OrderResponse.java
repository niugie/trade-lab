package com.tradelab.api.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.tradelab.domain.order.OrderStatus;
import com.tradelab.domain.order.TradeOrder;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class OrderResponse {

    /** Snowflake ID — serialized as string to avoid JS Number precision loss. */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long orderId;
    private Long userId;
    private Long skuId;
    private Integer quantity;
    private Long totalAmount;
    private Long discountAmount;
    private Long payAmount;
    private OrderStatus status;
    private Long userCouponId;
    private String idempotencyKey;
    private Instant expireAt;
    private Instant paidAt;
    private Instant createdAt;

    public static OrderResponse from(TradeOrder order) {
        return OrderResponse.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .skuId(order.getSkuId())
                .quantity(order.getQuantity())
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .payAmount(order.getPayAmount())
                .status(order.getStatus())
                .userCouponId(order.getUserCouponId())
                .idempotencyKey(order.getIdempotencyKey())
                .expireAt(order.getExpireAt())
                .paidAt(order.getPaidAt())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
