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
        emergencyRequestService.expireExpiredEmergencyRequests();
    }
}
