package com.tradelab.infrastructure.messaging;

import com.tradelab.application.order.OrderService;
import com.tradelab.application.outbox.OrderEventType;
import com.tradelab.domain.messaging.MessageConsumeLog;
import com.tradelab.infrastructure.persistence.MessageConsumeLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class OrderCloseConsumer {

    private static final String CONSUMER_NAME = "order-close-consumer";

    private final OrderService orderService;
    private final MessageConsumeLogRepository consumeLogRepository;

    @RabbitListener(queues = RabbitMqConfig.QUEUE_ORDER_CLOSE)
    @Transactional
    public void onMessage(Map<String, Object> envelope) {
        String messageId = envelope.get("outboxId") + ":" + envelope.get("eventType");
        if (consumeLogRepository.existsById(messageId)) {
            return;
        }

        try {
            consumeLogRepository.save(MessageConsumeLog.builder()
                    .messageId(messageId)
                    .consumer(CONSUMER_NAME)
                    .consumedAt(Instant.now())
                    .build());
        } catch (DataIntegrityViolationException e) {
            return;
        }

        String eventType = (String) envelope.get("eventType");
        if (!OrderEventType.ORDER_CREATED.equals(eventType)) {
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) envelope.get("payload");
        Long orderId = ((Number) payload.get("orderId")).longValue();
        log.info("Received ORDER_CREATED for order {}, scheduling close check via expire job", orderId);
    }
}
