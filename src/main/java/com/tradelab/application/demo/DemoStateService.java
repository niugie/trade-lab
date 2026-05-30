package com.tradelab.application.demo;

import com.tradelab.api.dto.CouponResponse;
import com.tradelab.api.dto.DemoStateResponse;
import com.tradelab.api.dto.InventoryResponse;
import com.tradelab.api.dto.OrderResponse;
import com.tradelab.common.BusinessException;
import com.tradelab.common.ErrorCode;
import com.tradelab.domain.coupon.CouponTemplate;
import com.tradelab.domain.coupon.UserCoupon;
import com.tradelab.domain.inventory.InventoryLedger;
import com.tradelab.domain.order.OrderStatus;
import com.tradelab.domain.order.TradeOrder;
import com.tradelab.infrastructure.persistence.CouponTemplateRepository;
import com.tradelab.infrastructure.persistence.InventoryLedgerRepository;
import com.tradelab.infrastructure.persistence.TradeOrderRepository;
import com.tradelab.infrastructure.persistence.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DemoStateService {

    public static final long DEMO_SKU_ID = 1L;
    public static final int SEED_AVAILABLE = 1000;
    public static final int SEED_RESERVED = 0;

    private final InventoryLedgerRepository inventoryLedgerRepository;
    private final UserCouponRepository userCouponRepository;
    private final CouponTemplateRepository couponTemplateRepository;
    private final TradeOrderRepository tradeOrderRepository;

    @Transactional(readOnly = true)
    public DemoStateResponse getState(long userId, long couponId, long skuId, int orderLimit) {
        InventoryLedger ledger = inventoryLedgerRepository.findById(skuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Inventory not found"));

        UserCoupon coupon = userCouponRepository.findByIdAndUserId(couponId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Coupon not found"));

        String templateName = couponTemplateRepository.findById(coupon.getTemplateId())
                .map(CouponTemplate::getName)
                .orElse("—");

        List<TradeOrder> orders = tradeOrderRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(0, orderLimit));

        List<TradeOrder> pendingOrders = tradeOrderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(
                userId, OrderStatus.PENDING_PAY, PageRequest.of(0, 20));

        long pendingCount = tradeOrderRepository.countByUserIdAndStatus(userId, OrderStatus.PENDING_PAY);

        return DemoStateResponse.builder()
                .inventory(InventoryResponse.from(ledger))
                .coupon(CouponResponse.from(coupon, templateName))
                .recentOrders(orders.stream().map(OrderResponse::from).toList())
                .pendingOrders(pendingOrders.stream().map(OrderResponse::from).toList())
                .pendingOrderCount((int) pendingCount)
                .build();
    }
}
