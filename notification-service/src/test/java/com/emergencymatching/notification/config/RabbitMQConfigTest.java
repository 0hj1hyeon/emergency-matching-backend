package com.emergencymatching.notification.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RabbitMQConfigTest {

    private final RabbitMQConfig rabbitMQConfig = new RabbitMQConfig();

    @Test
    void testQueueAndExchangeDefinitions() {
        // Exchange 검증
        TopicExchange exchange = rabbitMQConfig.emergencyExchange();
        assertThat(exchange.getName()).isEqualTo(RabbitMQConfig.EXCHANGE_NAME);

        // 생성 알림 큐 검증
        Queue requestQueue = rabbitMQConfig.emergencyRequestQueue();
        assertThat(requestQueue.getName()).isEqualTo(RabbitMQConfig.QUEUE_NAME);
        assertThat(requestQueue.getArguments().get("x-dead-letter-exchange")).isEqualTo(RabbitMQConfig.DLX_EXCHANGE_NAME);
        assertThat(requestQueue.getArguments().get("x-dead-letter-routing-key")).isEqualTo(RabbitMQConfig.ROUTING_KEY);

        // 수락 알림 큐 검증
        Queue acceptedQueue = rabbitMQConfig.emergencyAcceptedQueue();
        assertThat(acceptedQueue.getName()).isEqualTo(RabbitMQConfig.QUEUE_ACCEPTED_NAME);
        assertThat(acceptedQueue.getArguments().get("x-dead-letter-exchange")).isEqualTo(RabbitMQConfig.DLX_EXCHANGE_NAME);
        assertThat(acceptedQueue.getArguments().get("x-dead-letter-routing-key")).isEqualTo(RabbitMQConfig.ROUTING_KEY_ACCEPTED);

        // Dead Letter Exchange 검증
        TopicExchange dlx = rabbitMQConfig.deadLetterExchange();
        assertThat(dlx.getName()).isEqualTo(RabbitMQConfig.DLX_EXCHANGE_NAME);

        // Dead Letter Queue 검증
        Queue dlq = rabbitMQConfig.deadLetterQueue();
        assertThat(dlq.getName()).isEqualTo(RabbitMQConfig.DLQ_NAME);
    }

    @Test
    void testMessageRecovererThrowsAmqpRejectAndDontRequeueException() {
        MessageRecoverer recoverer = rabbitMQConfig.messageRecoverer();
        Message message = new Message("test-payload".getBytes(), new MessageProperties());
        Exception cause = new RuntimeException("Simulated processing error");

        // MessageRecoverer가 호출되면 로깅 후 AmqpRejectAndDontRequeueException을 던져 
        // RabbitMQ가 해당 메시지를 DLQ로 무리없이 보낼 수 있도록 예외 처리가 작동하는지 확인합니다.
        assertThrows(Exception.class, () -> {
            recoverer.recover(message, cause);
        });
    }
}
