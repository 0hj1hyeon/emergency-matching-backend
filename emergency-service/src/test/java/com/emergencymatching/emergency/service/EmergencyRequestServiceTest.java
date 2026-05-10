package com.emergencymatching.emergency.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.emergencymatching.emergency.domain.EmergencyRequest;
import com.emergencymatching.emergency.domain.EmergencyRequestStatus;
import com.emergencymatching.emergency.domain.PatientGender;
import com.emergencymatching.emergency.domain.SeverityLevel;
import com.emergencymatching.emergency.repository.EmergencyRequestRepository;
import com.emergencymatching.emergency.web.dto.CreateEmergencyRequestRequest;
import com.emergencymatching.emergency.web.dto.EmergencyRequestResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmergencyRequestServiceTest {

    @Mock
    private EmergencyRequestRepository emergencyRequestRepository;

    @InjectMocks
    private EmergencyRequestService emergencyRequestService;

    @Test
    void createEmergencyRequestSucceeds() {
        CreateEmergencyRequestRequest request = createRequest();
        given(emergencyRequestRepository.save(any(EmergencyRequest.class)))
                .willAnswer(invocation -> emergencyRequestWithId(invocation.getArgument(0), 1L));

        EmergencyRequestResponse response = emergencyRequestService.createEmergencyRequest(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.paramedicId()).isEqualTo(10L);
        assertThat(response.patientCondition()).isEqualTo("Chest pain and shortness of breath");
        assertThat(response.patientGender()).isEqualTo(PatientGender.MALE);
        assertThat(response.patientAgeGroup()).isEqualTo("60s");
        assertThat(response.severityLevel()).isEqualTo(SeverityLevel.CRITICAL);
        assertThat(response.latitude()).isEqualTo(37.5665);
        assertThat(response.longitude()).isEqualTo(126.9780);
    }

    @Test
    void createEmergencyRequestSavesRequestedStatus() {
        CreateEmergencyRequestRequest request = createRequest();
        given(emergencyRequestRepository.save(any(EmergencyRequest.class)))
                .willAnswer(invocation -> emergencyRequestWithId(invocation.getArgument(0), 1L));

        emergencyRequestService.createEmergencyRequest(request);

        ArgumentCaptor<EmergencyRequest> captor = ArgumentCaptor.forClass(EmergencyRequest.class);
        verify(emergencyRequestRepository).save(captor.capture());

        assertThat(captor.getValue().getStatus()).isEqualTo(EmergencyRequestStatus.REQUESTED);
    }

    @Test
    void createEmergencyRequestCreatesExpiresAt() {
        CreateEmergencyRequestRequest request = createRequest();
        given(emergencyRequestRepository.save(any(EmergencyRequest.class)))
                .willAnswer(invocation -> emergencyRequestWithId(invocation.getArgument(0), 1L));

        EmergencyRequestResponse response = emergencyRequestService.createEmergencyRequest(request);

        assertThat(response.expiresAt()).isNotNull();
        assertThat(response.expiresAt()).isAfter(response.createdAt());
    }

    @Test
    void createEmergencyRequestValidationRejectsInvalidValues() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        CreateEmergencyRequestRequest invalidRequest = new CreateEmergencyRequestRequest(
                null,
                "",
                null,
                "",
                null,
                91.0,
                181.0
        );

        Set<ConstraintViolation<CreateEmergencyRequestRequest>> violations =
                validator.validate(invalidRequest);

        assertThat(violations).hasSize(7);
    }

    private CreateEmergencyRequestRequest createRequest() {
        return new CreateEmergencyRequestRequest(
                10L,
                "Chest pain and shortness of breath",
                PatientGender.MALE,
                "60s",
                SeverityLevel.CRITICAL,
                37.5665,
                126.9780
        );
    }

    private EmergencyRequest emergencyRequestWithId(EmergencyRequest emergencyRequest, Long id) {
        LocalDateTime now = LocalDateTime.of(2026, 5, 10, 12, 0);
        ReflectionTestUtils.setField(emergencyRequest, "id", id);
        ReflectionTestUtils.setField(emergencyRequest, "createdAt", now);
        ReflectionTestUtils.setField(emergencyRequest, "updatedAt", now);
        ReflectionTestUtils.setField(emergencyRequest, "version", 0L);
        return emergencyRequest;
    }
}
