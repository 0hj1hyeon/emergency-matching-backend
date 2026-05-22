package com.emergencymatching.notification.event;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * RabbitMQ로부터 수신할 응급 이송 요청 생성 이벤트 DTO입니다.
 * 
 * emergency-service에서 발행한 JSON 메시지의 필드 구조(Key)와 일치해야
 * 역직렬화(Deserialization)가 올바르게 수행됩니다.
 */
public record EmergencyRequestCreatedEvent(
        Long requestId,
        Long paramedicId,
        List<Long> hospitalIds,
        String patientCondition,
        String patientGender,
        String patientAgeGroup,
        String severityLevel,
        Double latitude,
        Double longitude,
        LocalDateTime createdAt
) implements Serializable {
}
