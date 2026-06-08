package com.emergencymatching.emergency.repository;

import com.emergencymatching.emergency.domain.HospitalResponse;
import com.emergencymatching.emergency.domain.HospitalResponseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 특정 응급 환자 이송 요청에 대한 각 후보 병원의 응답 현황(대기, 수락, 거절 등)을 관리하는 Repository입니다.
 */
public interface HospitalResponseRepository extends JpaRepository<HospitalResponse, Long> {

    /**
     * 특정 응급 요청 ID와 특정 병원 ID를 기반으로 대기 또는 응답 완료된 상세 레코드를 단건 조회합니다.
     */
    Optional<HospitalResponse> findByEmergencyRequestIdAndHospitalId(Long emergencyRequestId, Long hospitalId);

    boolean existsByEmergencyRequestIdAndHospitalId(Long emergencyRequestId, Long hospitalId);

    /**
     * 특정 응급 요청 ID에 연동되어 있는 모든 후보 병원들의 응답 목록을 조회합니다.
     * (매칭 완료 시, 다른 후보 병원군을 대상으로 '마감 알림'을 전파하기 위해 사용됩니다.)
     */
    List<HospitalResponse> findByEmergencyRequestId(Long emergencyRequestId);

    List<HospitalResponse> findAllByEmergencyRequestIdInAndStatus(
            List<Long> emergencyRequestIds,
            HospitalResponseStatus status
    );

    /**
     * 특정 병원 ID와 상태를 기반으로 매칭 대기 중인 응답 목록을 조회합니다.
     */
    List<HospitalResponse> findAllByHospitalIdAndStatus(
            Long hospitalId,
            HospitalResponseStatus status
    );
}
