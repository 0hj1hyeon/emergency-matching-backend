package com.emergencymatching.emergency.client.dto;

public record HospitalResponseDto(
        Long hospitalId,
        String name,
        String address,
        Double latitude,
        Double longitude,
        Double distanceKm,
        Boolean isAvailable
) {
}
