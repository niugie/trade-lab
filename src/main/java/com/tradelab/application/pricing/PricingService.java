package com.tradelab.application.pricing;

import com.tradelab.common.BusinessException;
import com.tradelab.common.ErrorCode;
import com.tradelab.domain.coupon.CouponTemplate;
import com.tradelab.domain.coupon.CouponType;
import com.tradelab.domain.coupon.UserCoupon;
import com.tradelab.infrastructure.persistence.CouponTemplateRepository;
import com.tradelab.infrastructure.persistence.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final UserCouponRepository userCouponRepository;
    private final CouponTemplateRepository couponTemplateRepository;

    public record PriceResult(long totalAmount, long discountAmount, long payAmount, Long userCouponId) {}

    public PriceResult calculate(long userId, long unitPrice, int quantity, Long optionalCouponId) {
        long total = unitPrice * quantity;
        if (optionalCouponId == null) {
            return new PriceResult(total, 0, total, null);
        }

        UserCoupon coupon = userCouponRepository.findByIdAndUserId(optionalCouponId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_UNAVAILABLE));

        CouponTemplate template = couponTemplateRepository.findById(coupon.getTemplateId())
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_UNAVAILABLE));

        Instant now = Instant.now();
        if (now.isBefore(template.getValidFrom()) || now.isAfter(template.getValidTo())) {
            throw new BusinessException(ErrorCode.COUPON_UNAVAILABLE, "Coupon expired");
        }
        if (total < template.getMinAmount()) {
            throw new BusinessException(ErrorCode.COUPON_UNAVAILABLE, "Order amount below coupon minimum");
        }

        long discount = switch (template.getType()) {
            case FIXED -> Math.min(template.getDiscountValue(), total);
            case PERCENT -> total - (total * template.getDiscountValue() / 1000);
        };

        long pay = Math.max(total - discount, 1);
        return new PriceResult(total, discount, pay, optionalCouponId);
    }
}
