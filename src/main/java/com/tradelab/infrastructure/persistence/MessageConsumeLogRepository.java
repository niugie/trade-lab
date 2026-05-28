package com.tradelab.infrastructure.persistence;

import com.tradelab.domain.messaging.MessageConsumeLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageConsumeLogRepository extends JpaRepository<MessageConsumeLog, String> {
}
