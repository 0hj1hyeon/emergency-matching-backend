package com.emergencymatching.emergency.event;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 응급 이송 요청이 생성되었을 때 RabbitMQ로 발행할 이벤트 DTO(편지 봉투)입니다.
 * 
 * MSA 환경에서는 각 서비스가 독립적이므로, 타 서비스(Notification Service)에서 역직렬화하기 
 * 쉽도록 열거형(Enum) 대신 일반 문자열(String) 타입을 사용하여 데이터를 규격화했습니다.
 * 
 * - hospitalIds: 알림을 전송할 대상 후보 병원 ID 목록
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
