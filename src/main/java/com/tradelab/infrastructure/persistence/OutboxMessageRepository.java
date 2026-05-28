package com.tradelab.infrastructure.persistence;

import com.tradelab.domain.outbox.OutboxMessage;
import com.tradelab.domain.outbox.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, Long> {

    @Query("""
            SELECT o FROM OutboxMessage o
            WHERE o.status = :status AND o.nextRetryAt <= :now
            ORDER BY o.id ASC
            """)
    List<OutboxMessage> findPending(OutboxStatus status, Instant now);
}
