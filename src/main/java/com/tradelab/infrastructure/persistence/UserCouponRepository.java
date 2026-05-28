package com.tradelab.infrastructure.persistence;

import com.tradelab.domain.coupon.UserCoupon;
import com.tradelab.domain.coupon.UserCouponStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    @Modifying
    @Query("""
            UPDATE UserCoupon c SET c.status = 'FROZEN', c.frozenOrderId = :orderId
            WHERE c.id = :couponId AND c.userId = :userId AND c.status = 'AVAILABLE'
            """)
    int freeze(@Param("couponId") Long couponId, @Param("userId") Long userId, @Param("orderId") Long orderId);

    @Modifying
    @Query("""
            UPDATE UserCoupon c SET c.status = 'USED', c.usedAt = :usedAt
            WHERE c.id = :couponId AND c.status = 'FROZEN' AND c.frozenOrderId = :orderId
            """)
    int confirmUse(@Param("couponId") Long couponId, @Param("orderId") Long orderId, @Param("usedAt") Instant usedAt);

    @Modifying
    @Query("""
            UPDATE UserCoupon c SET c.status = 'AVAILABLE', c.frozenOrderId = NULL
            WHERE c.id = :couponId AND c.status = 'FROZEN' AND c.frozenOrderId = :orderId
            """)
    int release(@Param("couponId") Long couponId, @Param("orderId") Long orderId);

    Optional<UserCoupon> findByIdAndUserId(Long id, Long userId);

    boolean existsByIdAndUserIdAndStatus(Long id, Long userId, UserCouponStatus status);
}
