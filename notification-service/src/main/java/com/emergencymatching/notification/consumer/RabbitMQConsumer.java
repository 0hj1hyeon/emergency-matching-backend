package com.emergencymatching.notification.consumer;

import com.emergencymatching.notification.config.RabbitMQConfig;
import com.emergencymatching.notification.event.EmergencyRequestCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ 우체통(Queue)을 상시 감시하는 메시지 수신원(Consumer)입니다.
 * 
 * `@RabbitListener`를 붙여두면, 스프링이 백그라운드 스레드를 띄워 우체통을 지켜보다가
 * 편지가 도착하는 순간 이 메서드를 호출하여 내용을 파싱해 줍니다.
 */
@Slf4j
@Component
public class RabbitMQConsumer {

    /**
     * "emergency.request.queue" 우체통에 도착한 응급 환자 발생 편지를 받아 로그로 출력합니다.
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
    }
}
