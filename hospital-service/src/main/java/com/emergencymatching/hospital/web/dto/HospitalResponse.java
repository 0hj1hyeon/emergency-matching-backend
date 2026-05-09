package com.emergencymatching.hospital.web.dto;

import com.emergencymatching.hospital.domain.Hospital;
import java.time.LocalDateTime;

public record HospitalResponse(
        Long id,
        Long memberId,
        String name,
        String address,
        Double latitude,
        Double longitude,
        Boolean isAvailable,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static HospitalResponse from(Hospital hospital) {
        return new HospitalResponse(
                hospital.getId(),
                hospital.getMemberId(),
                hospital.getName(),
                hospital.getAddress(),
                hospital.getLatitude(),
                hospital.getLongitude(),
                hospital.getAvailable(),
                hospital.getCreatedAt(),
                hospital.getUpdatedAt()
        );
    }
}
