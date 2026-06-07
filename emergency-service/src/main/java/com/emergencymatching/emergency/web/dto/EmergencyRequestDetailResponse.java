package com.emergencymatching.emergency.web.dto;

import com.emergencymatching.emergency.domain.EmergencyRequest;
import com.emergencymatching.emergency.domain.EmergencyRequestStatus;
import com.emergencymatching.emergency.domain.HospitalResponse;
import com.emergencymatching.emergency.domain.HospitalResponseStatus;
import com.emergencymatching.emergency.domain.PatientGender;
import com.emergencymatching.emergency.domain.SeverityLevel;
import java.time.LocalDateTime;
import java.util.List;

public record EmergencyRequestDetailResponse(
        Long requestId,
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
        List<HospitalResponseDetail> hospitalResponses
) {

    public static EmergencyRequestDetailResponse from(
            EmergencyRequest emergencyRequest,
            List<HospitalResponse> hospitalResponses
    ) {
        return new EmergencyRequestDetailResponse(
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
                hospitalResponses.stream()
                        .map(HospitalResponseDetail::from)
                        .toList()
        );
    }

    public record HospitalResponseDetail(
            Long hospitalId,
            HospitalResponseStatus status,
            LocalDateTime respondedAt,
            LocalDateTime createdAt
    ) {

        public static HospitalResponseDetail from(HospitalResponse hospitalResponse) {
            return new HospitalResponseDetail(
                    hospitalResponse.getHospitalId(),
                    hospitalResponse.getStatus(),
                    hospitalResponse.getRespondedAt(),
                    hospitalResponse.getCreatedAt()
            );
        }
    }
}
