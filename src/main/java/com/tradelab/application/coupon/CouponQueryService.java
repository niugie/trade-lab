package com.tradelab.application.coupon;

import com.tradelab.api.dto.CouponResponse;
import com.tradelab.common.BusinessException;
import com.tradelab.common.ErrorCode;
import com.tradelab.domain.coupon.CouponTemplate;
import com.tradelab.domain.coupon.UserCoupon;
import com.tradelab.infrastructure.persistence.CouponTemplateRepository;
import com.tradelab.infrastructure.persistence.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponQueryService {

    private final UserCouponRepository userCouponRepository;
    private final CouponTemplateRepository couponTemplateRepository;

    @Transactional(readOnly = true)
    public CouponResponse getCoupon(long couponId, long userId) {
        UserCoupon coupon = userCouponRepository.findByIdAndUserId(couponId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Coupon not found"));

        String templateName = couponTemplateRepository.findById(coupon.getTemplateId())
                .map(CouponTemplate::getName)
                .orElse("—");

        return CouponResponse.from(coupon, templateName);
    }
}
