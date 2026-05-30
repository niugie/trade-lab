package com.tradelab.infrastructure.scheduling;

import com.tradelab.application.order.OrderService;
import com.tradelab.domain.order.OrderStatus;
import com.tradelab.domain.order.TradeOrder;
import com.tradelab.infrastructure.persistence.TradeOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@Profile("!test & !local")
@RequiredArgsConstructor
public class OrderExpireScheduler {

    private final TradeOrderRepository tradeOrderRepository;
    private final OrderService orderService;

    @Scheduled(fixedDelay = 10000)
    public void closeExpiredOrders() {
        List<TradeOrder> expired = tradeOrderRepository.findExpiredOrders(OrderStatus.PENDING_PAY, Instant.now());
        for (TradeOrder order : expired) {
            try {
                orderService.closeOrder(order.getId());
                log.info("Closed expired order {}", order.getId());
            } catch (Exception e) {
                log.warn("Failed to close order {}: {}", order.getId(), e.getMessage());
            }
        }
    }
}
