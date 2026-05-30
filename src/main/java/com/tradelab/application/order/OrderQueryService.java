package com.tradelab.application.order;

import com.tradelab.api.dto.OrderResponse;
import com.tradelab.domain.order.OrderStatus;
import com.tradelab.infrastructure.persistence.TradeOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final TradeOrderRepository tradeOrderRepository;

    @Transactional(readOnly = true)
    public List<OrderResponse> listByUser(long userId, int limit, OrderStatus status) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        var page = PageRequest.of(0, safeLimit);
        var orders = status == null
                ? tradeOrderRepository.findByUserIdOrderByCreatedAtDesc(userId, page)
                : tradeOrderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status, page);
        return orders.stream().map(OrderResponse::from).toList();
    }
}
