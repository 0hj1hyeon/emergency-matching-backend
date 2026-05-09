package com.emergencymatching.hospital.web.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateHospitalAvailabilityRequest(
        @NotNull
        Boolean isAvailable
) {
}
