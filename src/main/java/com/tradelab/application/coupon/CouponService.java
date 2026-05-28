package com.tradelab.application.coupon;

import com.tradelab.common.BusinessException;
import com.tradelab.common.ErrorCode;
import com.tradelab.infrastructure.persistence.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final UserCouponRepository userCouponRepository;

    @Transactional
    public void freeze(Long userId, Long couponId, Long orderId) {
        int updated = userCouponRepository.freeze(couponId, userId, orderId);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.COUPON_UNAVAILABLE);
        }
    }

    @Transactional
    public void confirm(Long couponId, Long orderId) {
        int updated = userCouponRepository.confirmUse(couponId, orderId, Instant.now());
        if (updated == 0) {
            throw new BusinessException(ErrorCode.COUPON_UNAVAILABLE, "Coupon confirm failed");
        }
    }

    @Transactional
    public void release(Long couponId, Long orderId) {
        userCouponRepository.release(couponId, orderId);
    }
}
