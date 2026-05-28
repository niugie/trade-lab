package com.tradelab.infrastructure.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class RabbitMqConfig {

    public static final String EXCHANGE_ORDER = "trade.order.exchange";
    public static final String QUEUE_ORDER_CLOSE = "trade.order.close.queue";
    public static final String ROUTING_ORDER_EVENT = "order.event";

    @Bean
    TopicExchange orderExchange() {
        return new TopicExchange(EXCHANGE_ORDER);
    }

    @Bean
    Queue orderCloseQueue() {
        return QueueBuilder.durable(QUEUE_ORDER_CLOSE).build();
    }

    @Bean
    Binding orderCloseBinding(Queue orderCloseQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderCloseQueue).to(orderExchange).with(ROUTING_ORDER_EVENT);
    }

    @Bean
    MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
