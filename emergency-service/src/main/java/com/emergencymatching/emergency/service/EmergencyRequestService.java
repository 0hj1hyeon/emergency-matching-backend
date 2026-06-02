package com.emergencymatching.emergency.service;

import com.emergencymatching.emergency.client.HospitalClient;
import com.emergencymatching.emergency.client.dto.HospitalResponseDto;
import com.emergencymatching.emergency.domain.EmergencyRequest;
import com.emergencymatching.emergency.domain.HospitalResponse;
import com.emergencymatching.emergency.event.EmergencyRequestCreatedEvent;
import com.emergencymatching.emergency.event.EmergencyRequestAcceptedEvent;
import com.emergencymatching.emergency.repository.EmergencyRequestRepository;
import com.emergencymatching.emergency.repository.HospitalResponseRepository;
import com.emergencymatching.emergency.web.dto.CreateEmergencyRequestRequest;
import com.emergencymatching.emergency.web.dto.CandidateHospitalResponse;
import com.emergencymatching.emergency.web.dto.EmergencyRequestResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
public class EmergencyRequestService {

    private static final double DEFAULT_NEARBY_HOSPITAL_RADIUS_KM = 5.0;

    private final EmergencyRequestRepository emergencyRequestRepository;
    private final HospitalResponseRepository hospitalResponseRepository;
    private final HospitalClient hospitalClient;
    private final RabbitTemplate rabbitTemplate;

    public EmergencyRequestService(
            EmergencyRequestRepository emergencyRequestRepository,
            HospitalResponseRepository hospitalResponseRepository,
            HospitalClient hospitalClient,
            RabbitTemplate rabbitTemplate
    ) {
        this.emergencyRequestRepository = emergencyRequestRepository;
        this.hospitalResponseRepository = hospitalResponseRepository;
        this.hospitalClient = hospitalClient;
        this.rabbitTemplate = rabbitTemplate;
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

        List<Long> hospitalIds = List.of();
        if (!nearbyHospitals.isEmpty()) {
            List<HospitalResponse> hospitalResponses = nearbyHospitals.stream()
                    .map(hospital -> HospitalResponse.pending(savedRequest.getId(), hospital.hospitalId()))
                    .toList();

            hospitalResponseRepository.saveAll(hospitalResponses);
            savedRequest.broadcast();

            hospitalIds = nearbyHospitals.stream()
                    .map(HospitalResponseDto::hospitalId)
                    .toList();
        }

        List<CandidateHospitalResponse> candidateHospitals = nearbyHospitals.stream()
                .map(CandidateHospitalResponse::from)
                .toList();

        // 3. RabbitMQ로 이벤트 발행 (상태가 BROADCASTED일 경우에만 이벤트 발행)
        if (savedRequest.getStatus() == com.emergencymatching.emergency.domain.EmergencyRequestStatus.BROADCASTED) {
            EmergencyRequestCreatedEvent event = new EmergencyRequestCreatedEvent(
                    savedRequest.getId(),
                    savedRequest.getParamedicId(),
                    hospitalIds,
                    savedRequest.getPatientCondition(),
                    savedRequest.getPatientGender().name(),
                    savedRequest.getPatientAgeGroup(),
                    savedRequest.getSeverityLevel().name(),
                    savedRequest.getLatitude(),
                    savedRequest.getLongitude(),
                    savedRequest.getCreatedAt() != null ? savedRequest.getCreatedAt() : java.time.LocalDateTime.now()
            );

            rabbitTemplate.convertAndSend("emergency.exchange", "emergency.request.created", event);
            log.info("응급 요청 생성 실시간 알림 비동기 이벤트 발행 완료 ➔ ID: {}, 전송 대상 병원 수: {}", savedRequest.getId(), hospitalIds.size());
        }

        return EmergencyRequestResponse.from(savedRequest, candidateHospitals);
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
            log.warn("Hospital Service 조회 실패 (Fallback 흐름 작동): {}", exception.getMessage());
            return List.of();
        }
    }

    private Double resolveNearbyHospitalRadiusKm(EmergencyRequest emergencyRequest) {
        return DEFAULT_NEARBY_HOSPITAL_RADIUS_KM;
    }

    @Transactional
    public void acceptEmergencyRequest(Long requestId, Long hospitalId) {
        // 1. 요청 조회
        EmergencyRequest emergencyRequest = emergencyRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("해당 응급 요청을 찾을 수 없습니다. ID: " + requestId));

        // 2. HospitalResponse 조회
        HospitalResponse hospitalResponse = hospitalResponseRepository.findByEmergencyRequestIdAndHospitalId(requestId, hospitalId)
                .orElseThrow(() -> new IllegalArgumentException("해당 후보 병원의 대기표를 찾을 수 없습니다. 병원 ID: " + hospitalId));

        // 3. 상태 변경 및 수락 처리
        hospitalResponse.accept();
        emergencyRequest.accept(hospitalId);

        // 4. 원래 포진했던 다른 후보 병원들 ID 목록을 구해서 알림 전파용으로 넘겨줌
        List<HospitalResponse> allResponses = hospitalResponseRepository.findByEmergencyRequestId(requestId);
        List<Long> hospitalIds = allResponses.stream()
                .map(HospitalResponse::getHospitalId)
                .toList();

        // 5. RabbitMQ로 수락 완료 비동기 이벤트 발행
        EmergencyRequestAcceptedEvent event = new EmergencyRequestAcceptedEvent(
                emergencyRequest.getId(),
                hospitalId,
                emergencyRequest.getParamedicId(),
                hospitalIds,
                emergencyRequest.getStatus().name(),
                java.time.LocalDateTime.now()
        );

        rabbitTemplate.convertAndSend("emergency.exchange", "emergency.request.accepted", event);
        log.info("응급 요청 수락 완료 비동기 이벤트 발행 완료 ➔ 요청 ID: {}, 수락 병원 ID: {}, 대상 병원 수: {}", requestId, hospitalId, hospitalIds.size());
    }

    @Transactional
    public void rejectEmergencyRequest(Long requestId, Long hospitalId) {
        // 1. HospitalResponse 조회
        HospitalResponse hospitalResponse = hospitalResponseRepository.findByEmergencyRequestIdAndHospitalId(requestId, hospitalId)
                .orElseThrow(() -> new IllegalArgumentException("해당 후보 병원의 대기표를 찾을 수 없습니다. 병원 ID: " + hospitalId));

        // 2. 거절 처리
        hospitalResponse.reject();
        log.info("응급 요청 거절 완료 ➔ 요청 ID: {}, 거절 병원 ID: {}", requestId, hospitalId);
    }
}
