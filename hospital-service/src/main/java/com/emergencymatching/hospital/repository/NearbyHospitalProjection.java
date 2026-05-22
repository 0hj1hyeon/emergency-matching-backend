package com.emergencymatching.hospital.repository;

public interface NearbyHospitalProjection {

    Long getHospitalId();

    String getName();

    String getAddress();

    Double getLatitude();

    Double getLongitude();

    Double getDistanceKm();

    Boolean getIsAvailable();
}
