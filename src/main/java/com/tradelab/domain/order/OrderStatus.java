package com.tradelab.domain.order;

import java.util.EnumSet;
import java.util.Set;

public enum OrderStatus {
    PENDING_PAY,
    PAID,
    CLOSED;

    private static final Set<OrderStatus> CAN_PAY = EnumSet.of(PENDING_PAY);
    private static final Set<OrderStatus> CAN_CLOSE = EnumSet.of(PENDING_PAY);

    public boolean canPay() {
        return CAN_PAY.contains(this);
    }

    public boolean canClose() {
        return CAN_CLOSE.contains(this);
    }
}
