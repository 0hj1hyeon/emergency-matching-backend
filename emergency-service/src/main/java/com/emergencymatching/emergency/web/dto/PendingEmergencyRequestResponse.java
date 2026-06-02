package com.emergencymatching.emergency.web.dto;

import com.emergencymatching.emergency.domain.EmergencyRequest;
import com.emergencymatching.emergency.domain.EmergencyRequestStatus;
import com.emergencymatching.emergency.domain.PatientGender;
import com.emergencymatching.emergency.domain.SeverityLevel;
import java.time.LocalDateTime;

public record PendingEmergencyRequestResponse(
        Long requestId,
        String patientCondition,
        PatientGender patientGender,
        String patientAgeGroup,
        SeverityLevel severityLevel,
        Double latitude,
        Double longitude,
        EmergencyRequestStatus status,
        LocalDateTime createdAt,
        LocalDateTime expiresAt
) {

    public static PendingEmergencyRequestResponse from(EmergencyRequest emergencyRequest) {
        return new PendingEmergencyRequestResponse(
                emergencyRequest.getId(),
                emergencyRequest.getPatientCondition(),
                emergencyRequest.getPatientGender(),
                emergencyRequest.getPatientAgeGroup(),
                emergencyRequest.getSeverityLevel(),
                emergencyRequest.getLatitude(),
                emergencyRequest.getLongitude(),
                emergencyRequest.getStatus(),
                emergencyRequest.getCreatedAt(),
                emergencyRequest.getExpiresAt()
        );
    }
}
