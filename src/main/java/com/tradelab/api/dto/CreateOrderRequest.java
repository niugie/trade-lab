package com.tradelab.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateOrderRequest {

    @NotNull
    private Long userId;

    @NotNull
    private Long skuId;

    @NotNull
    @Min(1)
    private Integer quantity;

    private Long userCouponId;

    @NotBlank
    private String idempotencyKey;
}
