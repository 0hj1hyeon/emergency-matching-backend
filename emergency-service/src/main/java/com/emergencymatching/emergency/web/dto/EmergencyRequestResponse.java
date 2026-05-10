package com.emergencymatching.emergency.web.dto;

import com.emergencymatching.emergency.domain.EmergencyRequest;
import com.emergencymatching.emergency.domain.EmergencyRequestStatus;
import com.emergencymatching.emergency.domain.PatientGender;
import com.emergencymatching.emergency.domain.SeverityLevel;
import java.time.LocalDateTime;

public record EmergencyRequestResponse(
        Long id,
        Long paramedicId,
        String patientCondition,
        PatientGender patientGender,
        String patientAgeGroup,
        SeverityLevel severityLevel,
        Double latitude,
        Double longitude,
        EmergencyRequestStatus status,
        Long acceptedHospitalId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime expiresAt,
        Long version
) {

    public static EmergencyRequestResponse from(EmergencyRequest emergencyRequest) {
        return new EmergencyRequestResponse(
                emergencyRequest.getId(),
                emergencyRequest.getParamedicId(),
                emergencyRequest.getPatientCondition(),
                emergencyRequest.getPatientGender(),
                emergencyRequest.getPatientAgeGroup(),
                emergencyRequest.getSeverityLevel(),
                emergencyRequest.getLatitude(),
                emergencyRequest.getLongitude(),
                emergencyRequest.getStatus(),
                emergencyRequest.getAcceptedHospitalId(),
                emergencyRequest.getCreatedAt(),
                emergencyRequest.getUpdatedAt(),
                emergencyRequest.getExpiresAt(),
                emergencyRequest.getVersion()
        );
    }
}
