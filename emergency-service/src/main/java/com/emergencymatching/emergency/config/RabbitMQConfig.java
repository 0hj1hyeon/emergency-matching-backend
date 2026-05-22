package com.emergencymatching.emergency.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * RabbitMQ 통신을 위한 설정 클래스입니다.
 * 
 * 우체국에 비유하자면, 편지 분류기(Exchange)와 개인 우체통(Queue),
 * 그리고 이 둘을 연결하는 배달 경로(Routing Key)를 등록하는 역할을 합니다.
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "emergency.exchange";
    public static final String QUEUE_NAME = "emergency.request.queue";
    public static final String ROUTING_KEY = "emergency.request.created";

    /**
     * 편지 분류기(Exchange)를 생성합니다.
     * Topic 방식을 사용하여 라우팅 키 패턴에 따라 알맞은 우체통으로 배달합니다.
     */
    @Bean
    public TopicExchange emergencyExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    /**
     * 우체통(Queue)을 생성합니다.
     * 이 우체통은 서버가 꺼져도 메일이 보존되도록 durable(지속성) 속성을 가집니다.
     */
    @Bean
    public Queue emergencyRequestQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    /**
     * 우체통과 편지 분류기를 주소지(Routing Key)로 연결해 주는 결합 규칙(Binding)을 생성합니다.
     * 
     * "분류기(Exchange)에 'emergency.request.created' 주소지가 적힌 편지가 오면,
     *  이 우체통(Queue)으로 쏙 넣어줘라!" 라는 규칙입니다.
     */
    @Bean
    public Binding binding(Queue emergencyRequestQueue, TopicExchange emergencyExchange) {
        return BindingBuilder.bind(emergencyRequestQueue)
                .to(emergencyExchange)
                .with(ROUTING_KEY);
    }

    /**
     * 객체를 JSON 형식으로 변환하여 안전하게 메시지 큐에 태우기 위한 컨버터입니다.
     * 
     * 기본 자바 직렬화 방식은 버전 관리나 타 프레임워크와의 호환이 어렵기 때문에,
     * 자바 객체를 텍스트 포맷인 JSON으로 변환하여 보내야 합니다.
     * LocalDateTime을 올바르게 파싱할 수 있도록 JavaTimeModule이 포함된 ObjectMapper를 연동합니다.
     */
    @Bean
    public MessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        // LocalDateTime 포맷 변환 오류를 방지하기 위해 JavaTimeModule을 등록한 ObjectMapper를 사용합니다.
        objectMapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
