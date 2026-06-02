package com.emergencymatching.emergency.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.emergencymatching.emergency.client.HospitalClient;
import com.emergencymatching.emergency.client.dto.HospitalResponseDto;
import com.emergencymatching.emergency.domain.EmergencyRequest;
import com.emergencymatching.emergency.domain.EmergencyRequestStatus;
import com.emergencymatching.emergency.domain.HospitalResponse;
import com.emergencymatching.emergency.domain.HospitalResponseStatus;
import com.emergencymatching.emergency.domain.PatientGender;
import com.emergencymatching.emergency.domain.SeverityLevel;
import com.emergencymatching.emergency.repository.EmergencyRequestRepository;
import com.emergencymatching.emergency.repository.HospitalResponseRepository;
import com.emergencymatching.emergency.web.dto.CreateEmergencyRequestRequest;
import com.emergencymatching.emergency.web.dto.EmergencyRequestResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmergencyRequestServiceTest {

    @Mock
    private EmergencyRequestRepository emergencyRequestRepository;

    @Mock
    private HospitalResponseRepository hospitalResponseRepository;

    @Mock
    private HospitalClient hospitalClient;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private EmergencyRequestService emergencyRequestService;

    @Test
    void createEmergencyRequestSucceeds() {
        CreateEmergencyRequestRequest request = createRequest();
        given(emergencyRequestRepository.save(any(EmergencyRequest.class)))
                .willAnswer(invocation -> emergencyRequestWithId(invocation.getArgument(0), 1L));
        given(hospitalClient.getNearbyHospitals(37.5665, 126.9780, 5.0))
                .willReturn(List.of(hospitalResponse(1L), hospitalResponse(2L)));

        EmergencyRequestResponse response = emergencyRequestService.createEmergencyRequest(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.paramedicId()).isEqualTo(10L);
        assertThat(response.patientCondition()).isEqualTo("Chest pain and shortness of breath");
        assertThat(response.patientGender()).isEqualTo(PatientGender.MALE);
        assertThat(response.patientAgeGroup()).isEqualTo("60s");
        assertThat(response.severityLevel()).isEqualTo(SeverityLevel.CRITICAL);
        assertThat(response.latitude()).isEqualTo(37.5665);
        assertThat(response.longitude()).isEqualTo(126.9780);
        assertThat(response.status()).isEqualTo(EmergencyRequestStatus.BROADCASTED);
        assertThat(response.candidateHospitals()).hasSize(2);

        // RabbitMQ 비동기 이벤트 발행 검증
        verify(rabbitTemplate).convertAndSend(
                eq("emergency.exchange"),
                eq("emergency.request.created"),
                any(com.emergencymatching.emergency.event.EmergencyRequestCreatedEvent.class)
        );
    }

    @Test
    void createEmergencyRequestCallsHospitalClient() {
        CreateEmergencyRequestRequest request = createRequest();
        given(emergencyRequestRepository.save(any(EmergencyRequest.class)))
                .willAnswer(invocation -> emergencyRequestWithId(invocation.getArgument(0), 1L));
        given(hospitalClient.getNearbyHospitals(any(), any(), any()))
                .willReturn(List.of(hospitalResponse(1L)));

        emergencyRequestService.createEmergencyRequest(request);

        verify(hospitalClient).getNearbyHospitals(
                eq(37.5665),
                eq(126.9780),
                eq(5.0)
        );
    }

    @Test
    void createEmergencyRequestCreatesPendingHospitalResponsesWhenCandidatesExist() {
        CreateEmergencyRequestRequest request = createRequest();
        given(emergencyRequestRepository.save(any(EmergencyRequest.class)))
                .willAnswer(invocation -> emergencyRequestWithId(invocation.getArgument(0), 1L));
        given(hospitalClient.getNearbyHospitals(37.5665, 126.9780, 5.0))
                .willReturn(List.of(hospitalResponse(10L), hospitalResponse(20L)));

        emergencyRequestService.createEmergencyRequest(request);

        ArgumentCaptor<Iterable<HospitalResponse>> captor = hospitalResponseIterableCaptor();
        verify(hospitalResponseRepository).saveAll(captor.capture());

        List<HospitalResponse> responses = toList(captor.getValue());
        assertThat(responses).hasSize(2);
        assertThat(responses)
                .extracting(HospitalResponse::getEmergencyRequestId)
                .containsOnly(1L);
        assertThat(responses)
                .extracting(HospitalResponse::getHospitalId)
                .containsExactly(10L, 20L);
        assertThat(responses)
                .extracting(HospitalResponse::getStatus)
                .containsOnly(HospitalResponseStatus.PENDING);
    }

    @Test
    void createEmergencyRequestChangesStatusToBroadcastedWhenCandidatesExist() {
        CreateEmergencyRequestRequest request = createRequest();
        given(emergencyRequestRepository.save(any(EmergencyRequest.class)))
                .willAnswer(invocation -> emergencyRequestWithId(invocation.getArgument(0), 1L));
        given(hospitalClient.getNearbyHospitals(37.5665, 126.9780, 5.0))
                .willReturn(List.of(hospitalResponse(1L)));

        emergencyRequestService.createEmergencyRequest(request);

        ArgumentCaptor<EmergencyRequest> captor = ArgumentCaptor.forClass(EmergencyRequest.class);
        verify(emergencyRequestRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(EmergencyRequestStatus.BROADCASTED);
    }

    @Test
    void createEmergencyRequestDoesNotCreateHospitalResponsesWhenCandidatesDoNotExist() {
        CreateEmergencyRequestRequest request = createRequest();
        given(emergencyRequestRepository.save(any(EmergencyRequest.class)))
                .willAnswer(invocation -> emergencyRequestWithId(invocation.getArgument(0), 1L));
        given(hospitalClient.getNearbyHospitals(37.5665, 126.9780, 5.0))
                .willReturn(List.of());

        EmergencyRequestResponse response = emergencyRequestService.createEmergencyRequest(request);

        verify(hospitalResponseRepository, never()).saveAll(any());
        assertThat(response.status()).isEqualTo(EmergencyRequestStatus.REQUESTED);
        assertThat(response.candidateHospitals()).isEmpty();
    }

    @Test
    void createEmergencyRequestKeepsRequestedStatusWhenHospitalClientFails() {
        CreateEmergencyRequestRequest request = createRequest();
        given(emergencyRequestRepository.save(any(EmergencyRequest.class)))
                .willAnswer(invocation -> emergencyRequestWithId(invocation.getArgument(0), 1L));
        given(hospitalClient.getNearbyHospitals(37.5665, 126.9780, 5.0))
                .willThrow(new RuntimeException("hospital-service unavailable"));

        EmergencyRequestResponse response = emergencyRequestService.createEmergencyRequest(request);

        verify(hospitalResponseRepository, never()).saveAll(any());
        assertThat(response.status()).isEqualTo(EmergencyRequestStatus.REQUESTED);
        assertThat(response.candidateHospitals()).isEmpty();
    }

    @Test
    void createEmergencyRequestCreatesExpiresAt() {
        CreateEmergencyRequestRequest request = createRequest();
        given(emergencyRequestRepository.save(any(EmergencyRequest.class)))
                .willAnswer(invocation -> emergencyRequestWithId(invocation.getArgument(0), 1L));
        given(hospitalClient.getNearbyHospitals(37.5665, 126.9780, 5.0))
                .willReturn(List.of());

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

    @Test
    void acceptEmergencyRequestSucceeds() {
        // given
        Long requestId = 1L;
        Long hospitalId = 10L;
        
        EmergencyRequest emergencyRequest = EmergencyRequest.create(
                10L, "Chest pain", PatientGender.MALE, "60s", SeverityLevel.CRITICAL, 37.5665, 126.9780
        );
        ReflectionTestUtils.setField(emergencyRequest, "id", requestId);
        emergencyRequest.broadcast(); // change status to BROADCASTED so it can be accepted
        
        HospitalResponse hospitalResponse = HospitalResponse.pending(requestId, hospitalId);
        
        given(emergencyRequestRepository.findById(requestId)).willReturn(java.util.Optional.of(emergencyRequest));
        given(hospitalResponseRepository.findByEmergencyRequestIdAndHospitalId(requestId, hospitalId))
                .willReturn(java.util.Optional.of(hospitalResponse));
        given(hospitalResponseRepository.findByEmergencyRequestId(requestId))
                .willReturn(List.of(hospitalResponse));
        
        // when
        emergencyRequestService.acceptEmergencyRequest(requestId, hospitalId);
        
        // then
        assertThat(emergencyRequest.getStatus()).isEqualTo(EmergencyRequestStatus.ACCEPTED);
        assertThat(emergencyRequest.getAcceptedHospitalId()).isEqualTo(hospitalId);
        assertThat(hospitalResponse.getStatus()).isEqualTo(HospitalResponseStatus.ACCEPTED);
        
        verify(rabbitTemplate).convertAndSend(
                eq("emergency.exchange"),
                eq("emergency.request.accepted"),
                any(com.emergencymatching.emergency.event.EmergencyRequestAcceptedEvent.class)
        );
    }

    @Test
    void rejectEmergencyRequestSucceeds() {
        // given
        Long requestId = 1L;
        Long hospitalId = 10L;
        HospitalResponse hospitalResponse = HospitalResponse.pending(requestId, hospitalId);
        
        given(hospitalResponseRepository.findByEmergencyRequestIdAndHospitalId(requestId, hospitalId))
                .willReturn(java.util.Optional.of(hospitalResponse));
        
        // when
        emergencyRequestService.rejectEmergencyRequest(requestId, hospitalId);
        
        // then
        assertThat(hospitalResponse.getStatus()).isEqualTo(HospitalResponseStatus.REJECTED);
        assertThat(hospitalResponse.getRespondedAt()).isNotNull();
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

    private HospitalResponseDto hospitalResponse(Long hospitalId) {
        return new HospitalResponseDto(
                hospitalId,
                "Seoul Emergency Hospital",
                "123 Seoul-ro",
                37.5665,
                126.9780,
                1.2,
                true
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<Iterable<HospitalResponse>> hospitalResponseIterableCaptor() {
        return ArgumentCaptor.forClass((Class) Iterable.class);
    }

    private List<HospitalResponse> toList(Iterable<HospitalResponse> hospitalResponses) {
        List<HospitalResponse> result = new ArrayList<>();
        hospitalResponses.forEach(result::add);
        return result;
    }
}
