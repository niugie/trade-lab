package com.tradelab.infrastructure.persistence;

import com.tradelab.domain.coupon.CouponTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponTemplateRepository extends JpaRepository<CouponTemplate, Long> {
}
