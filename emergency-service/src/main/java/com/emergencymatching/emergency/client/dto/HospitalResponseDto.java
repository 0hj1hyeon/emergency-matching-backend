package com.emergencymatching.emergency.client.dto;

/**
 * Hospital Service로부터 제공받을 근처 병원 정보 DTO입니다.
 * 4주차 회의록의 스펙에 맞추어 병원 ID, 이름, 환자와의 거리(km), 수용 가능 여부를 정의했습니다.
 */
public record HospitalResponseDto(
        Long hospitalId,
        String name,
        Double distanceKm,
        Boolean isAvailable
) {
}
