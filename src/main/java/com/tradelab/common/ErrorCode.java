package com.tradelab.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    SUCCESS(0, "OK"),
    BAD_REQUEST(40000, "Bad request"),
    NOT_FOUND(40400, "Resource not found"),
    INSUFFICIENT_STOCK(40901, "Insufficient stock"),
    COUPON_UNAVAILABLE(40902, "Coupon unavailable"),
    INVALID_ORDER_STATE(40903, "Invalid order state"),
    DUPLICATE_REQUEST(40904, "Duplicate request"),
    INTERNAL_ERROR(50000, "Internal error");

    private final int code;
    private final String message;
}
