package com.emergencymatching.emergency.scheduler;

import com.emergencymatching.emergency.service.EmergencyRequestService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EmergencyRequestExpirationScheduler {

    private final EmergencyRequestService emergencyRequestService;

    public EmergencyRequestExpirationScheduler(EmergencyRequestService emergencyRequestService) {
        this.emergencyRequestService = emergencyRequestService;
    }

    @Scheduled(fixedDelayString = "${emergency.expiration.scheduler.fixed-delay-ms:10000}")
    public void expireEmergencyRequests() {
        // MVP 단계에서는 단일 인스턴스 실행을 기준으로 스케줄러를 운영한다.
        // 다중 인스턴스 운영 시에는 ShedLock 또는 SELECT FOR UPDATE SKIP LOCKED 기반 중복 실행 방지 전략을 적용한다.
        emergencyRequestService.expireExpiredEmergencyRequests();
    }
}
