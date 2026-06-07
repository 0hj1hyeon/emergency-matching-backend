package com.emergencymatching.emergency.web;

import com.emergencymatching.emergency.service.EmergencyRequestService;
import com.emergencymatching.emergency.web.dto.CreateEmergencyRequestRequest;
import com.emergencymatching.emergency.web.dto.EmergencyRequestDetailResponse;
import com.emergencymatching.emergency.web.dto.EmergencyRequestResponse;
import com.emergencymatching.emergency.web.dto.HospitalActionRequest;
import com.emergencymatching.emergency.web.dto.PendingEmergencyRequestResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/emergency-requests")
public class EmergencyRequestController {

    private final EmergencyRequestService emergencyRequestService;

    public EmergencyRequestController(EmergencyRequestService emergencyRequestService) {
        this.emergencyRequestService = emergencyRequestService;
    }

    @PostMapping
    public ResponseEntity<EmergencyRequestResponse> createEmergencyRequest(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader("X-User-Role") String userRoleHeader,
            @Valid @RequestBody CreateEmergencyRequestRequest request
    ) {
        if (!"PARAMEDIC".equals(userRoleHeader)) {
            throw new IllegalArgumentException("구급대원 권한이 필요합니다.");
        }
        
        Long paramedicId = Long.valueOf(userIdHeader);
        // 바디의 paramedicId를 헤더에서 검증된 ID로 강제 보정하여 스푸핑 방어
        CreateEmergencyRequestRequest securedRequest = new CreateEmergencyRequestRequest(
                paramedicId,
                request.patientCondition(),
                request.patientGender(),
                request.patientAgeGroup(),
                request.severityLevel(),
                request.latitude(),
                request.longitude()
        );

        EmergencyRequestResponse response = emergencyRequestService.createEmergencyRequest(securedRequest);

        return ResponseEntity.created(URI.create("/api/emergency-requests/" + response.id()))
                .body(response);
    }

    @PostMapping("/{requestId}/accept")
    public ResponseEntity<Void> acceptEmergencyRequest(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader("X-User-Role") String userRoleHeader,
            @PathVariable("requestId") Long requestId,
            @Valid @RequestBody HospitalActionRequest request
    ) {
        if (!"HOSPITAL".equals(userRoleHeader)) {
            throw new IllegalArgumentException("병원 권한이 필요합니다.");
        }
        Long authHospitalId = Long.valueOf(userIdHeader);
        if (!authHospitalId.equals(request.hospitalId())) {
            throw new IllegalArgumentException("본인 병원의 요청만 수락할 수 있습니다.");
        }

        emergencyRequestService.acceptEmergencyRequest(requestId, request.hospitalId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{requestId}/reject")
    public ResponseEntity<Void> rejectEmergencyRequest(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader("X-User-Role") String userRoleHeader,
            @PathVariable("requestId") Long requestId,
            @Valid @RequestBody HospitalActionRequest request
    ) {
        if (!"HOSPITAL".equals(userRoleHeader)) {
            throw new IllegalArgumentException("병원 권한이 필요합니다.");
        }
        Long authHospitalId = Long.valueOf(userIdHeader);
        if (!authHospitalId.equals(request.hospitalId())) {
            throw new IllegalArgumentException("본인 병원의 요청만 거절할 수 있습니다.");
        }

        emergencyRequestService.rejectEmergencyRequest(requestId, request.hospitalId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/hospitals/{hospitalId}/pending")
    public ResponseEntity<List<PendingEmergencyRequestResponse>> getPendingRequestsByHospital(
            @RequestHeader("X-User-Id") String userIdHeader,
            @RequestHeader("X-User-Role") String userRoleHeader,
            @PathVariable Long hospitalId
    ) {
        if (!"HOSPITAL".equals(userRoleHeader)) {
            throw new IllegalArgumentException("병원 권한이 필요합니다.");
        }
        Long authHospitalId = Long.valueOf(userIdHeader);
        if (!authHospitalId.equals(hospitalId)) {
            throw new IllegalArgumentException("본인 병원의 대기 요청 목록만 조회할 수 있습니다.");
        }

        return ResponseEntity.ok(emergencyRequestService.getPendingRequestsByHospital(hospitalId));
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<EmergencyRequestDetailResponse> getEmergencyRequestDetail(
            @PathVariable Long requestId
    ) {
        return ResponseEntity.ok(emergencyRequestService.getEmergencyRequestDetail(requestId));
    }
}
