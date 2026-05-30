package com.tradelab.application.demo;

import com.tradelab.api.dto.DemoStateResponse;
import com.tradelab.application.order.OrderService;
import com.tradelab.domain.order.OrderStatus;
import com.tradelab.domain.order.TradeOrder;
import com.tradelab.infrastructure.persistence.InventoryLedgerRepository;
import com.tradelab.infrastructure.persistence.TradeOrderRepository;
import com.tradelab.infrastructure.persistence.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DemoResetService {

    public static final long DEMO_USER_ID = 10001L;
    public static final long DEMO_COUPON_ID = 1L;
    public static final long DEMO_SKU_ID = DemoStateService.DEMO_SKU_ID;

    private final TradeOrderRepository tradeOrderRepository;
    private final OrderService orderService;
    private final UserCouponRepository userCouponRepository;
    private final InventoryLedgerRepository inventoryLedgerRepository;
    private final DemoStateService demoStateService;

    @Transactional
    public DemoStateResponse reset() {
        List<TradeOrder> pending = tradeOrderRepository.findByUserIdAndStatus(DEMO_USER_ID, OrderStatus.PENDING_PAY);
        for (TradeOrder order : pending) {
            orderService.closeOrder(order.getId());
        }
        userCouponRepository.resetForDemo(DEMO_COUPON_ID, DEMO_USER_ID);
        inventoryLedgerRepository.resetForDemo(DEMO_SKU_ID, DemoStateService.SEED_AVAILABLE, DemoStateService.SEED_RESERVED);
        return demoStateService.getState(DEMO_USER_ID, DEMO_COUPON_ID, DEMO_SKU_ID, 5);
    }
}
