package com.emergencymatching.hospital.web.dto;

import com.emergencymatching.hospital.repository.NearbyHospitalProjection;

public record NearbyHospitalResponse(
        Long hospitalId,
        String name,
        String address,
        Double latitude,
        Double longitude,
        Double distanceKm,
        Boolean isAvailable
) {

    public static NearbyHospitalResponse from(NearbyHospitalProjection projection) {
        return new NearbyHospitalResponse(
                projection.getHospitalId(),
                projection.getName(),
                projection.getAddress(),
                projection.getLatitude(),
                projection.getLongitude(),
                projection.getDistanceKm(),
                projection.getIsAvailable()
        );
    }
}
