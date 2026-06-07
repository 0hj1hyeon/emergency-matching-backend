package com.emergencymatching.notification.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.security.Principal;
import java.util.Map;

/**
 * Notification Service의 WebSocket 및 STOMP 설정을 담당하는 클래스입니다.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Gateway에서 검증된 X-User-Id와 X-User-Role HTTP 헤더를 WebSocket 세션 속성으로 저장하는 인터셉터
        HttpSessionHandshakeInterceptor handshakeInterceptor = new HttpSessionHandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(
                    ServerHttpRequest request,
                    ServerHttpResponse response,
                    WebSocketHandler wsHandler,
                    Map<String, Object> attributes
            ) throws Exception {
                String userId = request.getHeaders().getFirst("X-User-Id");
                String userRole = request.getHeaders().getFirst("X-User-Role");
                
                if (userId != null) {
                    attributes.put("userId", userId);
                }
                if (userRole != null) {
                    attributes.put("userRole", userRole);
                }
                return super.beforeHandshake(request, response, wsHandler, attributes);
            }
        };

        // 1. SockJS 클라이언트 연결 엔드포인트
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(handshakeInterceptor)
                .withSockJS();
        
        // 2. SockJS 미사용 순수 WebSocket 클라이언트 연결 엔드포인트
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(handshakeInterceptor);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                
                if (accessor != null) {
                    // CONNECT 시점에 Principal 설정
                    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                        if (sessionAttributes != null) {
                            String userId = (String) sessionAttributes.get("userId");
                            String userRole = (String) sessionAttributes.get("userRole");
                            
                            if (userId != null && userRole != null) {
                                StompPrincipal principal = new StompPrincipal(userId, userRole);
                                accessor.setUser(principal);
                            }
                        }
                    }
                    
                    // SUBSCRIBE 시점에 채널 권한(도청 방지) 검증
                    if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                        String destination = accessor.getDestination();
                        if (destination != null) {
                            StompPrincipal principal = (StompPrincipal) accessor.getUser();
                            if (principal == null) {
                                throw new IllegalArgumentException("인증된 사용자만 구독할 수 있습니다.");
                            }

                            if (destination.startsWith("/topic/hospitals/")) {
                                // 형식: /topic/hospitals/{hospitalId}/requests
                                String[] parts = destination.split("/");
                                if (parts.length >= 4 && "requests".equals(parts[3])) {
                                    String pathHospitalId = parts[2];
                                    if (!"HOSPITAL".equals(principal.getRole()) || !principal.getName().equals(pathHospitalId)) {
                                        throw new IllegalArgumentException("해당 병원 채널에 대한 구독 권한이 없습니다.");
                                    }
                                }
                            } else if (destination.startsWith("/topic/emergency/")) {
                                // 형식: /topic/emergency/{requestId}/status
                                if (!"PARAMEDIC".equals(principal.getRole())) {
                                    throw new IllegalArgumentException("대원 알림 채널은 구급대원만 구독할 수 있습니다.");
                                }
                            }
                        }
                    }
                }
                return message;
            }
        });
    }
}
