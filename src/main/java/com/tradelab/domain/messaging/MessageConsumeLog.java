package com.tradelab.domain.messaging;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "message_consume_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageConsumeLog {

    @Id
    @Column(name = "message_id", length = 128)
    private String messageId;

    @Column(nullable = false, length = 64)
    private String consumer;

    @Column(name = "consumed_at", nullable = false, updatable = false)
    private Instant consumedAt;
}
