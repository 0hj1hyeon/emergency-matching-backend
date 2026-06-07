package com.emergencymatching.emergency.service;

import com.emergencymatching.emergency.client.HospitalClient;
import com.emergencymatching.emergency.client.dto.HospitalResponseDto;
import com.emergencymatching.emergency.domain.EmergencyRequest;
import com.emergencymatching.emergency.domain.EmergencyRequestStatus;
import com.emergencymatching.emergency.domain.HospitalResponse;
import com.emergencymatching.emergency.domain.HospitalResponseStatus;
import com.emergencymatching.emergency.event.EmergencyRequestAcceptedEvent;
import com.emergencymatching.emergency.event.EmergencyRequestCreatedEvent;
import com.emergencymatching.emergency.exception.ConflictException;
import com.emergencymatching.emergency.exception.InvalidRequestException;
import com.emergencymatching.emergency.exception.ResourceNotFoundException;
import com.emergencymatching.emergency.repository.EmergencyRequestRepository;
import com.emergencymatching.emergency.repository.HospitalResponseRepository;
import com.emergencymatching.emergency.web.dto.CreateEmergencyRequestRequest;
import com.emergencymatching.emergency.web.dto.CandidateHospitalResponse;
import com.emergencymatching.emergency.web.dto.EmergencyRequestDetailResponse;
import com.emergencymatching.emergency.web.dto.EmergencyRequestResponse;
import com.emergencymatching.emergency.web.dto.PendingEmergencyRequestResponse;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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

        if (savedRequest.getStatus() == EmergencyRequestStatus.BROADCASTED) {
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

            publishAfterCommit("emergency.request.created", event);
            log.info("응급 요청 생성 실시간 알림 비동기 이벤트 발행 예약 ➔ ID: {}, 전송 대상 병원 수: {}", savedRequest.getId(), hospitalIds.size());
        }

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

    public EmergencyRequestDetailResponse getEmergencyRequestDetail(Long requestId) {
        EmergencyRequest emergencyRequest = emergencyRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 응급 요청을 찾을 수 없습니다. ID: " + requestId));
        List<HospitalResponse> hospitalResponses = hospitalResponseRepository.findByEmergencyRequestId(requestId);

        return EmergencyRequestDetailResponse.from(emergencyRequest, hospitalResponses);
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
        EmergencyRequest emergencyRequest = emergencyRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 응급 요청을 찾을 수 없습니다. ID: " + requestId));

        HospitalResponse hospitalResponse = hospitalResponseRepository.findByEmergencyRequestIdAndHospitalId(requestId, hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 후보 병원의 대기표를 찾을 수 없습니다. 병원 ID: " + hospitalId));

        if (emergencyRequest.getExpiresAt() != null && LocalDateTime.now().isAfter(emergencyRequest.getExpiresAt())) {
            throw new InvalidRequestException("만료된 요청은 수락할 수 없습니다.");
        }

        if (emergencyRequest.getStatus() == EmergencyRequestStatus.ACCEPTED) {
            throw new ConflictException("이미 수락된 요청입니다.");
        }

        if (emergencyRequest.getStatus() != EmergencyRequestStatus.BROADCASTED) {
            throw new InvalidRequestException("수락 가능한 상태가 아닙니다.");
        }

        if (hospitalResponse.getStatus() != HospitalResponseStatus.PENDING) {
            throw new InvalidRequestException("이미 응답 처리된 병원입니다.");
        }

        hospitalResponse.accept();
        emergencyRequest.accept(hospitalId);

        hospitalResponseRepository.save(hospitalResponse);
        emergencyRequestRepository.save(emergencyRequest);

        List<Long> closedHospitalIds = hospitalResponseRepository.findByEmergencyRequestId(requestId)
                .stream()
                .map(HospitalResponse::getHospitalId)
                .filter(candidateHospitalId -> !candidateHospitalId.equals(hospitalId))
                .toList();

        EmergencyRequestAcceptedEvent event = new EmergencyRequestAcceptedEvent(
                emergencyRequest.getId(),
                hospitalId,
                emergencyRequest.getParamedicId(),
                closedHospitalIds,
                emergencyRequest.getStatus().name(),
                hospitalResponse.getRespondedAt()
        );
        publishAfterCommit("emergency.request.accepted", event);
        log.info("응급 요청 수락 이벤트 발행 예약 ➔ 요청 ID: {}, 수락 병원 ID: {}, 마감 병원 수: {}",
                requestId, hospitalId, closedHospitalIds.size());
    }

    @Transactional
    public void rejectEmergencyRequest(Long requestId, Long hospitalId) {
        EmergencyRequest emergencyRequest = emergencyRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 응급 요청을 찾을 수 없습니다. ID: " + requestId));

        HospitalResponse hospitalResponse = hospitalResponseRepository.findByEmergencyRequestIdAndHospitalId(requestId, hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 후보 병원의 대기표를 찾을 수 없습니다. 병원 ID: " + hospitalId));

        if (emergencyRequest.getExpiresAt() != null && LocalDateTime.now().isAfter(emergencyRequest.getExpiresAt())) {
            throw new InvalidRequestException("만료된 요청은 거절할 수 없습니다.");
        }

        if (emergencyRequest.getStatus() == EmergencyRequestStatus.ACCEPTED) {
            throw new ConflictException("이미 수락된 요청입니다.");
        }

        if (emergencyRequest.getStatus() != EmergencyRequestStatus.BROADCASTED) {
            throw new InvalidRequestException("거절 가능한 상태가 아닙니다.");
        }

        if (hospitalResponse.getStatus() != HospitalResponseStatus.PENDING) {
            throw new InvalidRequestException("이미 응답 처리된 병원입니다.");
        }

        hospitalResponse.reject();
        hospitalResponseRepository.save(hospitalResponse);
        log.info("응급 요청 거절 완료 ➔ 요청 ID: {}, 거절 병원 ID: {}", requestId, hospitalId);
    }

    private void publishAfterCommit(String routingKey, Object event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    rabbitTemplate.convertAndSend("emergency.exchange", routingKey, event);
                }
            });
            return;
        }

        rabbitTemplate.convertAndSend("emergency.exchange", routingKey, event);
    }
}
