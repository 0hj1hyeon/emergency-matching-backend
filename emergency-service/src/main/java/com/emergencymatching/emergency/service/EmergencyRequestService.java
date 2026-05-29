package com.emergencymatching.emergency.service;

import com.emergencymatching.emergency.client.HospitalClient;
import com.emergencymatching.emergency.client.dto.HospitalResponseDto;
import com.emergencymatching.emergency.domain.EmergencyRequest;
import com.emergencymatching.emergency.domain.HospitalResponse;
import com.emergencymatching.emergency.domain.HospitalResponseStatus;
import com.emergencymatching.emergency.repository.EmergencyRequestRepository;
import com.emergencymatching.emergency.repository.HospitalResponseRepository;
import com.emergencymatching.emergency.web.dto.CreateEmergencyRequestRequest;
import com.emergencymatching.emergency.web.dto.CandidateHospitalResponse;
import com.emergencymatching.emergency.web.dto.EmergencyRequestResponse;
import com.emergencymatching.emergency.web.dto.PendingEmergencyRequestResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class EmergencyRequestService {

    private static final double DEFAULT_NEARBY_HOSPITAL_RADIUS_KM = 5.0;

    private final EmergencyRequestRepository emergencyRequestRepository;
    private final HospitalResponseRepository hospitalResponseRepository;
    private final HospitalClient hospitalClient;

    public EmergencyRequestService(
            EmergencyRequestRepository emergencyRequestRepository,
            HospitalResponseRepository hospitalResponseRepository,
            HospitalClient hospitalClient
    ) {
        this.emergencyRequestRepository = emergencyRequestRepository;
        this.hospitalResponseRepository = hospitalResponseRepository;
        this.hospitalClient = hospitalClient;
    }

    @Transactional
    public EmergencyRequestResponse createEmergencyRequest(CreateEmergencyRequestRequest request) {
        EmergencyRequest emergencyRequest = EmergencyRequest.create(
                request.paramedicId(),
                request.patientCondition(),
                request.patientGender(),
                request.patientAgeGroup(),
                request.severityLevel(),
                request.latitude(),
                request.longitude()
        );

        EmergencyRequest savedRequest = emergencyRequestRepository.save(emergencyRequest);

        List<HospitalResponseDto> nearbyHospitals = getNearbyHospitals(savedRequest);

        if (!nearbyHospitals.isEmpty()) {
            List<HospitalResponse> hospitalResponses = nearbyHospitals.stream()
                    .map(hospital -> HospitalResponse.pending(savedRequest.getId(), hospital.hospitalId()))
                    .toList();

            hospitalResponseRepository.saveAll(hospitalResponses);
            savedRequest.broadcast();
        }

        List<CandidateHospitalResponse> candidateHospitals = nearbyHospitals.stream()
                .map(CandidateHospitalResponse::from)
                .toList();

        return EmergencyRequestResponse.from(savedRequest, candidateHospitals);
    }

    public List<PendingEmergencyRequestResponse> getPendingRequestsByHospital(Long hospitalId) {
        List<Long> emergencyRequestIds = hospitalResponseRepository.findAllByHospitalIdAndStatus(
                        hospitalId,
                        HospitalResponseStatus.PENDING
                )
                .stream()
                .map(HospitalResponse::getEmergencyRequestId)
                .toList();

        if (emergencyRequestIds.isEmpty()) {
            return List.of();
        }

        return emergencyRequestRepository.findAllByIdIn(emergencyRequestIds)
                .stream()
                .map(PendingEmergencyRequestResponse::from)
                .toList();
    }

    private List<HospitalResponseDto> getNearbyHospitals(EmergencyRequest emergencyRequest) {
        try {
            List<HospitalResponseDto> nearbyHospitals = hospitalClient.getNearbyHospitals(
                    emergencyRequest.getLatitude(),
                    emergencyRequest.getLongitude(),
                    resolveNearbyHospitalRadiusKm(emergencyRequest)
            );

            // 후보 병원이 없으면 병원에 전파할 대상이 없으므로 REQUESTED 상태를 유지한다.
            return nearbyHospitals != null ? nearbyHospitals : List.of();
        } catch (Exception exception) {
            // hospital-service 장애가 있어도 응급 요청 기록은 보존한다.
            // 후보 병원 조회 실패는 후보 없음과 동일하게 다루며 REQUESTED 상태를 유지한다.
            return List.of();
        }
    }

    private Double resolveNearbyHospitalRadiusKm(EmergencyRequest emergencyRequest) {
        return DEFAULT_NEARBY_HOSPITAL_RADIUS_KM;
    }
}
