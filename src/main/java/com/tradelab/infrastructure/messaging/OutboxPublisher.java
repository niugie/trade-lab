package com.tradelab.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradelab.domain.outbox.OutboxMessage;
import com.tradelab.domain.outbox.OutboxStatus;
import com.tradelab.infrastructure.persistence.OutboxMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxMessageRepository outboxMessageRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${trade.outbox.batch-size:50}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${trade.outbox.poll-interval-ms:3000}")
    @Transactional
    public void publishPending() {
        List<OutboxMessage> pending = outboxMessageRepository.findPending(OutboxStatus.PENDING, Instant.now());
        int count = 0;
        for (OutboxMessage msg : pending) {
            if (count >= batchSize) {
                break;
            }
            try {
                Map<String, Object> envelope = new HashMap<>();
                envelope.put("outboxId", msg.getId());
                envelope.put("eventType", msg.getEventType());
                envelope.put("aggregateType", msg.getAggregateType());
                envelope.put("aggregateId", msg.getAggregateId());
                JsonNode payload = objectMapper.readTree(msg.getPayload());
                envelope.put("payload", payload);

                rabbitTemplate.convertAndSend(
                        RabbitMqConfig.EXCHANGE_ORDER,
                        RabbitMqConfig.ROUTING_ORDER_EVENT,
                        envelope
                );

                msg.setStatus(OutboxStatus.SENT);
                msg.setSentAt(Instant.now());
                count++;
            } catch (Exception e) {
                log.warn("Outbox publish failed id={}: {}", msg.getId(), e.getMessage());
                msg.setRetryCount(msg.getRetryCount() + 1);
                msg.setNextRetryAt(Instant.now().plusSeconds(Math.min(60, msg.getRetryCount() * 5L)));
                if (msg.getRetryCount() >= 10) {
                    msg.setStatus(OutboxStatus.FAILED);
                }
            }
        }
    }
}
