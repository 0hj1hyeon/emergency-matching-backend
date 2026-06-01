package com.emergencymatching.notification.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Notification Service의 WebSocket 및 STOMP 설정을 담당하는 클래스입니다.
 * 
 * - @EnableWebSocketMessageBroker: 메시지 브로커가 지원하는 WebSocket 메시지 처리를 활성화합니다.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 내장 심플 브로커가 /topic으로 시작하는 채널 구독자들에게 메시지를 전송하도록 활성화합니다.
        // /topic/hospitals/... 이나 /topic/emergency/... 채널 구독을 처리합니다.
        config.enableSimpleBroker("/topic");
        
        // 클라이언트에서 서버로 메시지를 보낼 때 사용하는 목적지 접두사를 설정합니다.
        // 현재 실시간 양방향 매칭은 백엔드 내 비동기 푸시 위주이므로 실제 송신 빈도는 낮지만 표준 설정을 정의해 둡니다.
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 1. SockJS 클라이언트를 위한 연결 엔드포인트를 /ws 로 설정합니다.
        // CORS 에러 방지를 위해 허용 패턴을 와일드카드(*)로 개방해 줍니다.
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
        
        // 2. SockJS를 사용하지 않는 순수 WebSocket 클라이언트 연결을 위해 동일 엔드포인트를 함께 선언합니다.
        // 다양한 프론트엔드 라이브러리(stompjs 등) 연동 신뢰성을 극대화합니다.
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }
}
