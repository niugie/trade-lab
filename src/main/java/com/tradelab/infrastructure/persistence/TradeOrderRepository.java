package com.tradelab.infrastructure.persistence;

import com.tradelab.domain.order.OrderStatus;
import com.tradelab.domain.order.TradeOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TradeOrderRepository extends JpaRepository<TradeOrder, Long> {

    Optional<TradeOrder> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    Optional<TradeOrder> findByPayNo(String payNo);

    @Query("""
            SELECT o FROM TradeOrder o
            WHERE o.status = :status AND o.expireAt <= :now
            ORDER BY o.expireAt ASC
            """)
    List<TradeOrder> findExpiredOrders(OrderStatus status, Instant now);
}
