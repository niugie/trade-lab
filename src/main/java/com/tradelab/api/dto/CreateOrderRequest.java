package com.tradelab.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "创建订单请求")
public class CreateOrderRequest {

    @Schema(description = "用户 ID", example = "10001")
    @NotNull
    private Long userId;

    @Schema(description = "商品 SKU", example = "1")
    @NotNull
    private Long skuId;

    @Schema(description = "购买数量", example = "1")
    @NotNull
    @Min(1)
    private Integer quantity;

    @Schema(description = "用户优惠券 ID，不用券则不传", example = "1")
    private Long userCouponId;

    @Schema(description = "幂等键，同一用户重复提交返回同一订单", example = "demo-001")
    @NotBlank
    private String idempotencyKey;
}
