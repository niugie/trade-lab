package com.tradelab.api;

import com.tradelab.api.dto.CreateOrderRequest;
import com.tradelab.api.dto.OrderResponse;
import com.tradelab.api.dto.PayOrderRequest;
import com.tradelab.application.order.OrderQueryService;
import com.tradelab.application.order.OrderService;
import com.tradelab.common.ApiResponse;
import com.tradelab.common.BusinessException;
import com.tradelab.common.ErrorCode;
import com.tradelab.domain.order.OrderStatus;
import com.tradelab.domain.order.TradeOrder;
import com.tradelab.infrastructure.persistence.TradeOrderRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "订单")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderQueryService orderQueryService;
    private final TradeOrderRepository tradeOrderRepository;

    @Operation(summary = "订单列表", description = "按用户查询订单，可选 status=PENDING_PAY 筛选待支付")
    @GetMapping
    public ApiResponse<java.util.List<OrderResponse>> list(
            @Parameter(description = "用户 ID", example = "10001") @RequestParam Long userId,
            @Parameter(description = "返回条数", example = "10") @RequestParam(defaultValue = "10") int limit,
            @Parameter(description = "按状态筛选") @RequestParam(required = false) OrderStatus status) {
        return ApiResponse.ok(orderQueryService.listByUser(userId, limit, status));
    }

    @Operation(summary = "创建订单")
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

    @Operation(summary = "查询订单")
    @GetMapping("/{orderId}")
    public ApiResponse<OrderResponse> get(@PathVariable Long orderId) {
        TradeOrder order = tradeOrderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Order not found"));
        return ApiResponse.ok(OrderResponse.from(order));
    }

    @Operation(summary = "支付订单")
    @PostMapping("/{orderId}/pay")
    public ApiResponse<OrderResponse> pay(@PathVariable Long orderId,
                                          @Valid @RequestBody PayOrderRequest request) {
        TradeOrder order = orderService.pay(orderId, request.getPayNo());
        return ApiResponse.ok(OrderResponse.from(order));
    }

    @Operation(summary = "关闭订单")
    @PostMapping("/{orderId}/close")
    public ApiResponse<OrderResponse> close(@PathVariable Long orderId) {
        TradeOrder order = orderService.closeOrder(orderId);
        return ApiResponse.ok(OrderResponse.from(order));
    }
}
