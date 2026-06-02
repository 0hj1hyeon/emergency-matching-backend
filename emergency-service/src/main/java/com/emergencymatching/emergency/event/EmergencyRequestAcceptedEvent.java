package com.emergencymatching.emergency.event;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 특정 병원이 응급 요청을 성공적으로 수락하여 매칭이 성사되었을 때 발행할 이벤트 DTO입니다.
 * 
 * - closedHospitalIds: 수락 병원을 제외한 후보 병원 ID 목록 (다른 병원들의 화면에 실시간 마감 처리를 전파하기 위해 포함)
 * - status: 현재 성사된 매칭 상태 (예: "ACCEPTED")
 */
public record EmergencyRequestAcceptedEvent(
        Long emergencyRequestId,
        Long acceptedHospitalId,
        Long paramedicId,
        List<Long> closedHospitalIds,
        String status,
        LocalDateTime acceptedAt
) implements Serializable {
}
