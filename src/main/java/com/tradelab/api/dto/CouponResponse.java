package com.tradelab.api.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.tradelab.domain.coupon.UserCoupon;
import com.tradelab.domain.coupon.UserCouponStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class CouponResponse {

    private Long couponId;
    private Long userId;
    private UserCouponStatus status;
    private String templateName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long frozenOrderId;

    private Instant usedAt;

    public static CouponResponse from(UserCoupon coupon, String templateName) {
        return CouponResponse.builder()
                .couponId(coupon.getId())
                .userId(coupon.getUserId())
                .status(coupon.getStatus())
                .templateName(templateName)
                .frozenOrderId(coupon.getFrozenOrderId())
                .usedAt(coupon.getUsedAt())
                .build();
    }
}
