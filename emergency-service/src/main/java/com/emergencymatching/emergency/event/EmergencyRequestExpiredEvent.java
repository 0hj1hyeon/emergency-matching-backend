package com.emergencymatching.emergency.event;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public record EmergencyRequestExpiredEvent(
        Long emergencyRequestId,
        Long paramedicId,
        List<Long> expiredHospitalIds,
        String status,
        LocalDateTime expiredAt
) implements Serializable {
}
