package com.emergencymatching.notification.event;

import java.io.Serializable;

/**
 * 실시간 WebSocket을 통해 병원 대시보드로 특정 응급 요청의 상태 변경(수락 완료/마감 등)을 전파하는 DTO입니다.
 */
public record HospitalRequestStatusUpdate(
        Long requestId,
        String status
) implements Serializable {
}
