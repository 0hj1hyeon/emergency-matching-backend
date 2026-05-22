package com.emergencymatching.hospital.web.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record NearbyHospitalSearchRequest(
        @NotNull
        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        Double lat,

        @NotNull
        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        Double lng,

        @NotNull
        @Positive
        Double radiusKm
) {
}
