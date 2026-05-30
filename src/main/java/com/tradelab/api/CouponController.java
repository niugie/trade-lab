package com.tradelab.api;

import com.tradelab.api.dto.CouponResponse;
import com.tradelab.application.coupon.CouponQueryService;
import com.tradelab.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "优惠券")
@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponQueryService couponQueryService;

    @Operation(summary = "查询用户优惠券")
    @GetMapping("/{couponId}")
    public ApiResponse<CouponResponse> get(@PathVariable Long couponId,
                                           @RequestParam Long userId) {
        return ApiResponse.ok(couponQueryService.getCoupon(couponId, userId));
    }
}
