package com.emergencymatching.notification.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Notification Service의 RabbitMQ 설정 클래스입니다.
 * 
 * 수신 대기할 우체통(Queue)의 정보와 들어오는 JSON 메시지를
 * 자바 객체(DTO)로 변환해 주는 메시지 컨버터를 정의합니다.
 * 또한 재시도 실패 시 메시지를 격리하는 DLQ(Dead Letter Queue)를 정의합니다.
 */
@Slf4j
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "emergency.exchange";
    
    // 응급 요청 생성 이벤트 관련 설정
    public static final String QUEUE_NAME = "emergency.request.queue";
    public static final String ROUTING_KEY = "emergency.request.created";

    // 응급 요청 수락 완료 이벤트 관련 설정
    public static final String QUEUE_ACCEPTED_NAME = "emergency.accepted.queue";
    public static final String ROUTING_KEY_ACCEPTED = "emergency.request.accepted";

    // Dead Letter Exchange 및 Queue 관련 설정
    public static final String DLX_EXCHANGE_NAME = "emergency.deadletter.exchange";
    public static final String DLQ_NAME = "emergency.deadletter.queue";

    @Bean
    public TopicExchange emergencyExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue emergencyRequestQueue() {
        return QueueBuilder.durable(QUEUE_NAME)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE_NAME)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding binding(Queue emergencyRequestQueue, TopicExchange emergencyExchange) {
        return BindingBuilder.bind(emergencyRequestQueue)
                .to(emergencyExchange)
                .with(ROUTING_KEY);
    }

    @Bean
    public Queue emergencyAcceptedQueue() {
        return QueueBuilder.durable(QUEUE_ACCEPTED_NAME)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE_NAME)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_ACCEPTED)
                .build();
    }

    @Bean
    public Binding bindingAccepted(Queue emergencyAcceptedQueue, TopicExchange emergencyExchange) {
        return BindingBuilder.bind(emergencyAcceptedQueue)
                .to(emergencyExchange)
                .with(ROUTING_KEY_ACCEPTED);
    }

    // --- Dead Letter Exchange & Queue 설정 ---

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(DLX_EXCHANGE_NAME);
    }

    @Bean
    public Queue deadLetterQueue() {
        return new Queue(DLQ_NAME, true);
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with("#"); // 모든 라우팅 키 수용
    }

    /**
     * 재시도가 최종 실패했을 때(3회 시도 초과),
     * 에러 원인과 메시지 바디(Payload)를 에러 로그로 남기고 메시지를 DLQ로 보냅니다.
     */
    @Bean
    public MessageRecoverer messageRecoverer() {
        return new RejectAndDontRequeueRecoverer() {
            @Override
            public void recover(org.springframework.amqp.core.Message message, Throwable cause) {
                log.error("RabbitMQ message retries exhausted. Sending to DLQ. Payload: {}, Error: {}",
                        new String(message.getBody()), cause.getMessage(), cause);
                super.recover(message, cause);
            }
        };
    }

    /**
     * RabbitMQ로부터 들어온 JSON 텍스트 메시지를 자바 객체(Event DTO)로 
     * 자동 역직렬화(Deserialization)해 주기 위한 컨버터 설정입니다.
     */
    @Bean
    public MessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        // LocalDateTime 역직렬화 오류를 막기 위해 JavaTimeModule을 등록해 줍니다.
        objectMapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
