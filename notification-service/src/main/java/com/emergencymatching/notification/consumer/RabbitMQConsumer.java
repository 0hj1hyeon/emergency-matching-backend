package com.emergencymatching.notification.consumer;

import com.emergencymatching.notification.config.RabbitMQConfig;
import com.emergencymatching.notification.event.EmergencyRequestCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ 우체통(Queue)을 상시 감시하는 메시지 수신원(Consumer)입니다.
 * 
 * `@RabbitListener`를 통해 수신한 메시지를 내부 메모리에 머무르지 않고,
 * WebSocket 실시간 중계 브로커를 활용해 브라우저(병원 및 대원)로 쏘아 올리는 핵심 브릿지 역할을 수행합니다.
 */
@Slf4j
@Component
public class RabbitMQConsumer {

    private final SimpMessagingTemplate messagingTemplate;

    public RabbitMQConsumer(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * "emergency.request.queue" 우체통에 도착한 응급 환자 생성 편지를 수신하여 로그를 기록하고,
     * 해당 이송 대상에 포진된 인근 병원들의 실시간 WebSocket 채널로 비동기 중계(Push)합니다.
     * 
     * @param event 응급 이송 요청 생성 이벤트 정보
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void consumeEmergencyRequest(EmergencyRequestCreatedEvent event) {
        log.info("========================================= [RabbitMQ 수신] =========================================");
        log.info("새로운 응급 환자 이송 요청 이벤트를 수신했습니다!");
        log.info(" - 요청 ID: {}", event.requestId());
        log.info(" - 구급대원 ID: {}", event.paramedicId());
        log.info(" - 환자 상태 요약: {}", event.patientCondition());
        log.info(" - 환자 성별: {}", event.patientGender());
        log.info(" - 환자 연령대: {}", event.patientAgeGroup());
        log.info(" - 중증도 Level: {}", event.severityLevel());
        log.info(" - 위치 위도/경도: {}, {}", event.latitude(), event.longitude());
        log.info(" - 알림 대상 후보 병원 수: {}", event.hospitalIds() != null ? event.hospitalIds().size() : 0);
        log.info(" - 알림 대상 병원 ID 목록: {}", event.hospitalIds());
        log.info(" - 요청 시각: {}", event.createdAt());
        log.info("===================================================================================================");

        // 실시간 WebSocket 전파망 작동
        if (event.hospitalIds() != null && !event.hospitalIds().isEmpty()) {
            for (Long hospitalId : event.hospitalIds()) {
                String destination = "/topic/hospitals/" + hospitalId + "/requests";
                messagingTemplate.convertAndSend(destination, event);
                log.info("[WebSocket Push] 병원 ID {} 의 실시간 채널로 응급 상황 발송 완료 ➔ {}", hospitalId, destination);
            }
        } else {
            log.warn("알림 대상 병원이 지정되지 않아 WebSocket Push가 스킵되었습니다. (요청 ID: {})", event.requestId());
        }
    }
}
