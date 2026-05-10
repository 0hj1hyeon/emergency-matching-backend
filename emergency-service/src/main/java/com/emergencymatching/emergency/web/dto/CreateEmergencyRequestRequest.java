package com.emergencymatching.emergency.web.dto;

import com.emergencymatching.emergency.domain.PatientGender;
import com.emergencymatching.emergency.domain.SeverityLevel;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateEmergencyRequestRequest(
        @NotNull
        @Positive
        Long paramedicId,

        @NotBlank
        String patientCondition,

        @NotNull
        PatientGender patientGender,

        @NotBlank
        String patientAgeGroup,

        @NotNull
        SeverityLevel severityLevel,

        @NotNull
        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        Double latitude,

        @NotNull
        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        Double longitude
) {
}
