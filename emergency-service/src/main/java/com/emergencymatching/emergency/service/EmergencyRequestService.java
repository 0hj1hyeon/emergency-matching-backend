package com.emergencymatching.emergency.service;

import com.emergencymatching.emergency.client.HospitalServiceClient;
import com.emergencymatching.emergency.client.dto.HospitalResponseDto;
import com.emergencymatching.emergency.domain.EmergencyRequest;
import com.emergencymatching.emergency.domain.HospitalResponse;
import com.emergencymatching.emergency.event.EmergencyRequestCreatedEvent;
import com.emergencymatching.emergency.repository.EmergencyRequestRepository;
import com.emergencymatching.emergency.repository.HospitalResponseRepository;
import com.emergencymatching.emergency.web.dto.CreateEmergencyRequestRequest;
import com.emergencymatching.emergency.web.dto.EmergencyRequestResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
public class EmergencyRequestService {

    private final EmergencyRequestRepository emergencyRequestRepository;
    private final HospitalResponseRepository hospitalResponseRepository;
    private final HospitalServiceClient hospitalServiceClient;
    private final RabbitTemplate rabbitTemplate;

    public EmergencyRequestService(
            EmergencyRequestRepository emergencyRequestRepository,
            HospitalResponseRepository hospitalResponseRepository,
            HospitalServiceClient hospitalServiceClient,
            RabbitTemplate rabbitTemplate
    ) {
        this.emergencyRequestRepository = emergencyRequestRepository;
        this.hospitalResponseRepository = hospitalResponseRepository;
        this.hospitalServiceClient = hospitalServiceClient;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Transactional
    public EmergencyRequestResponse createEmergencyRequest(CreateEmergencyRequestRequest request) {
        // 1. 구급요청 생성 및 일차 저장
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

        // 2. Feign Client로 근처 병원 목록 조회 (검색 반경 기본값: 5.0km)
        // 병원 서비스 장애 시에도 응급 요청 저장은 성공할 수 있도록 예외처리(장애 복원성)
        List<Long> hospitalIds = Collections.emptyList();
        try {
            List<HospitalResponseDto> nearbyHospitals = hospitalServiceClient.getNearbyHospitals(
                    savedRequest.getLatitude(),
                    savedRequest.getLongitude(),
                    5.0
            );
            if (nearbyHospitals != null) {
                hospitalIds = nearbyHospitals.stream()
                        .map(HospitalResponseDto::hospitalId)
                        .toList();
            }
        } catch (Exception e) {
            log.warn("Hospital Service 조회 실패 (장애 대체 복구 흐름 작동): {}", e.getMessage());
        }

        // 2-1. 후보 병원별로 PENDING 상태의 응답 레코드를 생성하여 DB에 저장
        if (!hospitalIds.isEmpty()) {
            for (Long hospitalId : hospitalIds) {
                HospitalResponse pendingResponse = HospitalResponse.pending(savedRequest.getId(), hospitalId);
                hospitalResponseRepository.save(pendingResponse);
            }
            log.info("응급 요청 ID {} 에 대한 후보 병원 {}개 응답 대기 레코드(PENDING) 생성 완료", savedRequest.getId(), hospitalIds.size());
        }

        // 3. 응급 요청 상태를 BROADCASTED(병원들로 알림 전송됨) 상태로 변경
        // @Transactional 내부이므로 객체의 상태 필드를 변경하면 더티 체킹에 의해 DB에 자동 반영됩니다.
        savedRequest.broadcast();

        // 4. RabbitMQ로 이벤트 발행 (JSON 직렬화 및 비동기 전송)
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
        log.info("응급 요청 생성 비동기 이벤트 발행 완료 - ID: {}, 전송된 후보 병원 수: {}", savedRequest.getId(), hospitalIds.size());

        return EmergencyRequestResponse.from(savedRequest);
    }
}
