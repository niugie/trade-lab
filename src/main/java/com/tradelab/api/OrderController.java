package com.tradelab.api;

import com.tradelab.api.dto.CreateOrderRequest;
import com.tradelab.api.dto.OrderResponse;
import com.tradelab.api.dto.PayOrderRequest;
import com.tradelab.application.order.OrderService;
import com.tradelab.common.ApiResponse;
import com.tradelab.common.BusinessException;
import com.tradelab.common.ErrorCode;
import com.tradelab.domain.order.TradeOrder;
import com.tradelab.infrastructure.persistence.TradeOrderRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final TradeOrderRepository tradeOrderRepository;

    @PostMapping
    public ApiResponse<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        TradeOrder order = orderService.createOrder(
                request.getUserId(),
                request.getSkuId(),
                request.getQuantity(),
                request.getUserCouponId(),
                request.getIdempotencyKey()
        );
        return ApiResponse.ok(OrderResponse.from(order));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> get(@PathVariable Long orderId) {
        TradeOrder order = tradeOrderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Order not found"));
        return ApiResponse.ok(OrderResponse.from(order));
    }

    @PostMapping("/{orderId}/pay")
    public ApiResponse<OrderResponse> pay(@PathVariable Long orderId,
                                          @Valid @RequestBody PayOrderRequest request) {
        TradeOrder order = orderService.pay(orderId, request.getPayNo());
        return ApiResponse.ok(OrderResponse.from(order));
    }

    @PostMapping("/{orderId}/close")
    public ApiResponse<OrderResponse> close(@PathVariable Long orderId) {
        TradeOrder order = orderService.closeOrder(orderId);
        return ApiResponse.ok(OrderResponse.from(order));
    }
}
