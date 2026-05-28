package com.tradelab.application.outbox;

public final class OrderEventType {

    public static final String ORDER_CREATED = "ORDER_CREATED";
    public static final String ORDER_PAID = "ORDER_PAID";
    public static final String ORDER_CLOSE_REQUESTED = "ORDER_CLOSE_REQUESTED";

    private OrderEventType() {}
}
