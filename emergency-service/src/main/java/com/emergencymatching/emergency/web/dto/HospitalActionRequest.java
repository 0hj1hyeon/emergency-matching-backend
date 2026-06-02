package com.emergencymatching.emergency.web.dto;

import jakarta.validation.constraints.NotNull;

public record HospitalActionRequest(
        @NotNull Long hospitalId
) {
}
