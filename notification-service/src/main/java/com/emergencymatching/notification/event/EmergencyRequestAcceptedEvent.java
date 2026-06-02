package com.emergencymatching.notification.event;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 특정 병원이 응급 요청을 성공적으로 수락하여 매칭이 성사되었을 때 수신받을 이벤트 DTO입니다.
 * 
 * - hospitalIds: 구급대원이 보낸 응급 요청의 원본 후보 병원 ID 목록 (다른 병원들의 화면에 실시간 마감 처리를 전파하기 위해 포함)
 * - status: 현재 성사된 매칭 상태 (예: "ACCEPTED")
 */
public record EmergencyRequestAcceptedEvent(
        Long requestId,
        Long hospitalId,
        Long paramedicId,
        List<Long> hospitalIds,
        String status,
        LocalDateTime acceptedAt
) implements Serializable {
}
