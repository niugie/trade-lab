package com.tradelab.api.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DemoStateResponse {

    private InventoryResponse inventory;
    private CouponResponse coupon;
    private List<OrderResponse> recentOrders;
    private List<OrderResponse> pendingOrders;
    private int pendingOrderCount;
}
