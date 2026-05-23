package com.emergencymatching.emergency.web.dto;

import com.emergencymatching.emergency.client.dto.HospitalResponseDto;

public record CandidateHospitalResponse(
        Long hospitalId,
        String name,
        String address,
        Double latitude,
        Double longitude,
        Double distanceKm,
        Boolean isAvailable
) {

    public static CandidateHospitalResponse from(HospitalResponseDto hospital) {
        return new CandidateHospitalResponse(
                hospital.hospitalId(),
                hospital.name(),
                hospital.address(),
                hospital.latitude(),
                hospital.longitude(),
                hospital.distanceKm(),
                hospital.isAvailable()
        );
    }
}
