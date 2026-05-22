package com.emergencymatching.notification.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Notification Service의 RabbitMQ 설정 클래스입니다.
 * 
 * 수신 대기할 우체통(Queue)의 정보와 들어오는 JSON 메시지를
 * 자바 객체(DTO)로 변환해 주는 메시지 컨버터를 정의합니다.
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "emergency.exchange";
    public static final String QUEUE_NAME = "emergency.request.queue";
    public static final String ROUTING_KEY = "emergency.request.created";

    @Bean
    public TopicExchange emergencyExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue emergencyRequestQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public Binding binding(Queue emergencyRequestQueue, TopicExchange emergencyExchange) {
        return BindingBuilder.bind(emergencyRequestQueue)
                .to(emergencyExchange)
                .with(ROUTING_KEY);
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
