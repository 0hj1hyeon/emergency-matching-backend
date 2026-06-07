package com.emergencymatching.emergency.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import com.emergencymatching.emergency.event.EmergencyRequestAcceptedEvent;
import com.emergencymatching.emergency.exception.ForbiddenException;
import com.emergencymatching.emergency.exception.ResourceNotFoundException;
import com.emergencymatching.emergency.repository.EmergencyRequestRepository;
import com.emergencymatching.emergency.repository.HospitalResponseRepository;
import com.emergencymatching.emergency.web.dto.CreateEmergencyRequestRequest;
import com.emergencymatching.emergency.web.dto.EmergencyRequestDetailResponse;
import com.emergencymatching.emergency.web.dto.EmergencyRequestResponse;
import com.emergencymatching.emergency.web.dto.PendingEmergencyRequestResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

        // 이벤트 발행은 이번 작업에서 검증하지 않음
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
                .willReturn(List.of(
                        hospitalResponse,
                        HospitalResponse.pending(requestId, 20L),
                        HospitalResponse.pending(requestId, 30L)
                ));
        
        // when
        emergencyRequestService.acceptEmergencyRequest(requestId, hospitalId);
        
        // then
        assertThat(emergencyRequest.getStatus()).isEqualTo(EmergencyRequestStatus.ACCEPTED);
        assertThat(emergencyRequest.getAcceptedHospitalId()).isEqualTo(hospitalId);
        assertThat(hospitalResponse.getStatus()).isEqualTo(HospitalResponseStatus.ACCEPTED);
        assertThat(hospitalResponse.getRespondedAt()).isNotNull();

        ArgumentCaptor<EmergencyRequestAcceptedEvent> eventCaptor =
                ArgumentCaptor.forClass(EmergencyRequestAcceptedEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq("emergency.exchange"),
                eq("emergency.request.accepted"),
                eventCaptor.capture()
        );
        EmergencyRequestAcceptedEvent event = eventCaptor.getValue();
        assertThat(event.requestId()).isEqualTo(requestId);
        assertThat(event.hospitalId()).isEqualTo(hospitalId);
        assertThat(event.paramedicId()).isEqualTo(10L);
        assertThat(event.hospitalIds()).containsExactly(20L, 30L);
        assertThat(event.status()).isEqualTo("ACCEPTED");
        assertThat(event.acceptedAt()).isNotNull();
    }

    @Test
    void acceptEmergencyRequestFailsWhenExpired() {
        // given
        Long requestId = 1L;
        Long hospitalId = 10L;
        
        EmergencyRequest emergencyRequest = EmergencyRequest.create(
                10L, "Chest pain", PatientGender.MALE, "60s", SeverityLevel.CRITICAL, 37.5665, 126.9780
        );
        ReflectionTestUtils.setField(emergencyRequest, "id", requestId);
        ReflectionTestUtils.setField(emergencyRequest, "expiresAt", LocalDateTime.now().minusSeconds(1));
        emergencyRequest.broadcast(); // change status to BROADCASTED so it can be accepted
        
        HospitalResponse hospitalResponse = HospitalResponse.pending(requestId, hospitalId);
        
        given(emergencyRequestRepository.findById(requestId)).willReturn(java.util.Optional.of(emergencyRequest));
        given(hospitalResponseRepository.findByEmergencyRequestIdAndHospitalId(requestId, hospitalId))
                .willReturn(java.util.Optional.of(hospitalResponse));
        
        // when & then
        assertThatThrownBy(() ->
                emergencyRequestService.acceptEmergencyRequest(requestId, hospitalId)
        )
                .isInstanceOf(com.emergencymatching.emergency.exception.InvalidRequestException.class)
                .hasMessageContaining("만료된 요청은 수락할 수 없습니다.");
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void rejectEmergencyRequestSucceeds() {
        // given
        Long requestId = 1L;
        Long hospitalId = 10L;
        EmergencyRequest emergencyRequest = EmergencyRequest.create(
                10L, "Chest pain", PatientGender.MALE, "60s", SeverityLevel.CRITICAL, 37.5665, 126.9780
        );
        ReflectionTestUtils.setField(emergencyRequest, "id", requestId);
        emergencyRequest.broadcast();

        HospitalResponse hospitalResponse = HospitalResponse.pending(requestId, hospitalId);
        
        given(emergencyRequestRepository.findById(requestId)).willReturn(Optional.of(emergencyRequest));
        given(hospitalResponseRepository.findByEmergencyRequestIdAndHospitalId(requestId, hospitalId))
                .willReturn(Optional.of(hospitalResponse));
        
        // when
        emergencyRequestService.rejectEmergencyRequest(requestId, hospitalId);
        
        // then
        assertThat(emergencyRequest.getStatus()).isEqualTo(EmergencyRequestStatus.BROADCASTED);
        assertThat(hospitalResponse.getStatus()).isEqualTo(HospitalResponseStatus.REJECTED);
        assertThat(hospitalResponse.getRespondedAt()).isNotNull();
    }

    @Test
    void acceptEmergencyRequestFailsWhenAlreadyAccepted() {
        Long requestId = 1L;
        Long hospitalId = 10L;

        EmergencyRequest emergencyRequest = EmergencyRequest.create(
                10L, "Chest pain", PatientGender.MALE, "60s", SeverityLevel.CRITICAL, 37.5665, 126.9780
        );
        ReflectionTestUtils.setField(emergencyRequest, "id", requestId);
        // set status to ACCEPTED to simulate already accepted
        ReflectionTestUtils.setField(emergencyRequest, "status", EmergencyRequestStatus.ACCEPTED);

        HospitalResponse hospitalResponse = HospitalResponse.pending(requestId, hospitalId);

        given(emergencyRequestRepository.findById(requestId)).willReturn(java.util.Optional.of(emergencyRequest));
        given(hospitalResponseRepository.findByEmergencyRequestIdAndHospitalId(requestId, hospitalId))
                .willReturn(java.util.Optional.of(hospitalResponse));

        assertThatThrownBy(() ->
                emergencyRequestService.acceptEmergencyRequest(requestId, hospitalId)
        ).isInstanceOf(com.emergencymatching.emergency.exception.ConflictException.class);
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void acceptEmergencyRequestFailsWhenNotCandidate() {
        Long requestId = 1L;
        Long hospitalId = 99L; // not a candidate

        EmergencyRequest emergencyRequest = EmergencyRequest.create(
                10L, "Chest pain", PatientGender.MALE, "60s", SeverityLevel.CRITICAL, 37.5665, 126.9780
        );
        ReflectionTestUtils.setField(emergencyRequest, "id", requestId);
        emergencyRequest.broadcast();

        given(emergencyRequestRepository.findById(requestId)).willReturn(java.util.Optional.of(emergencyRequest));
        given(hospitalResponseRepository.findByEmergencyRequestIdAndHospitalId(requestId, hospitalId))
                .willReturn(java.util.Optional.empty());

        assertThatThrownBy(() ->
                emergencyRequestService.acceptEmergencyRequest(requestId, hospitalId)
        ).isInstanceOf(com.emergencymatching.emergency.exception.ResourceNotFoundException.class);
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void acceptEmergencyRequestFailsWhenHospitalResponseIsNotPending() {
        Long requestId = 1L;
        Long hospitalId = 10L;
        EmergencyRequest emergencyRequest = broadcastedEmergencyRequest(requestId);
        HospitalResponse hospitalResponse = HospitalResponse.pending(requestId, hospitalId);
        hospitalResponse.reject();

        given(emergencyRequestRepository.findById(requestId)).willReturn(Optional.of(emergencyRequest));
        given(hospitalResponseRepository.findByEmergencyRequestIdAndHospitalId(requestId, hospitalId))
                .willReturn(Optional.of(hospitalResponse));

        assertThatThrownBy(() -> emergencyRequestService.acceptEmergencyRequest(requestId, hospitalId))
                .isInstanceOf(com.emergencymatching.emergency.exception.InvalidRequestException.class);
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void rejectEmergencyRequestFailsWhenAlreadyAccepted() {
        Long requestId = 1L;
        Long hospitalId = 10L;
        EmergencyRequest emergencyRequest = broadcastedEmergencyRequest(requestId);
        ReflectionTestUtils.setField(emergencyRequest, "status", EmergencyRequestStatus.ACCEPTED);
        HospitalResponse hospitalResponse = HospitalResponse.pending(requestId, hospitalId);

        given(emergencyRequestRepository.findById(requestId)).willReturn(Optional.of(emergencyRequest));
        given(hospitalResponseRepository.findByEmergencyRequestIdAndHospitalId(requestId, hospitalId))
                .willReturn(Optional.of(hospitalResponse));

        assertThatThrownBy(() -> emergencyRequestService.rejectEmergencyRequest(requestId, hospitalId))
                .isInstanceOf(com.emergencymatching.emergency.exception.ConflictException.class);
    }

    @Test
    void rejectEmergencyRequestFailsWhenExpired() {
        Long requestId = 1L;
        Long hospitalId = 10L;
        EmergencyRequest emergencyRequest = broadcastedEmergencyRequest(requestId);
        ReflectionTestUtils.setField(emergencyRequest, "expiresAt", LocalDateTime.now().minusSeconds(1));
        HospitalResponse hospitalResponse = HospitalResponse.pending(requestId, hospitalId);

        given(emergencyRequestRepository.findById(requestId)).willReturn(Optional.of(emergencyRequest));
        given(hospitalResponseRepository.findByEmergencyRequestIdAndHospitalId(requestId, hospitalId))
                .willReturn(Optional.of(hospitalResponse));

        assertThatThrownBy(() -> emergencyRequestService.rejectEmergencyRequest(requestId, hospitalId))
                .isInstanceOf(com.emergencymatching.emergency.exception.InvalidRequestException.class);
    }

    @Test
    void rejectEmergencyRequestFailsWhenRequestIsNotBroadcasted() {
        Long requestId = 1L;
        Long hospitalId = 10L;
        EmergencyRequest emergencyRequest = EmergencyRequest.create(
                10L, "Chest pain", PatientGender.MALE, "60s", SeverityLevel.CRITICAL, 37.5665, 126.9780
        );
        ReflectionTestUtils.setField(emergencyRequest, "id", requestId);
        HospitalResponse hospitalResponse = HospitalResponse.pending(requestId, hospitalId);

        given(emergencyRequestRepository.findById(requestId)).willReturn(Optional.of(emergencyRequest));
        given(hospitalResponseRepository.findByEmergencyRequestIdAndHospitalId(requestId, hospitalId))
                .willReturn(Optional.of(hospitalResponse));

        assertThatThrownBy(() -> emergencyRequestService.rejectEmergencyRequest(requestId, hospitalId))
                .isInstanceOf(com.emergencymatching.emergency.exception.InvalidRequestException.class);
    }

    @Test
    void rejectEmergencyRequestFailsWhenHospitalResponseIsNotPending() {
        Long requestId = 1L;
        Long hospitalId = 10L;
        EmergencyRequest emergencyRequest = broadcastedEmergencyRequest(requestId);
        HospitalResponse hospitalResponse = HospitalResponse.pending(requestId, hospitalId);
        hospitalResponse.accept();

        given(emergencyRequestRepository.findById(requestId)).willReturn(Optional.of(emergencyRequest));
        given(hospitalResponseRepository.findByEmergencyRequestIdAndHospitalId(requestId, hospitalId))
                .willReturn(Optional.of(hospitalResponse));

        assertThatThrownBy(() -> emergencyRequestService.rejectEmergencyRequest(requestId, hospitalId))
                .isInstanceOf(com.emergencymatching.emergency.exception.InvalidRequestException.class);
    }

    @Test
    void getPendingRequestsByHospitalReturnsOnlyPendingRequestsForHospital() {
        given(hospitalResponseRepository.findAllByHospitalIdAndStatus(100L, HospitalResponseStatus.PENDING))
                .willReturn(List.of(
                        hospitalResponseEntity(1L, 100L),
                        hospitalResponseEntity(2L, 100L)
                ));
        given(emergencyRequestRepository.findAllByIdIn(List.of(1L, 2L)))
                .willReturn(List.of(
                        emergencyRequest(1L, "Chest pain and shortness of breath"),
                        emergencyRequest(2L, "Severe abdominal pain")
                ));

        List<PendingEmergencyRequestResponse> responses =
                emergencyRequestService.getPendingRequestsByHospital(100L);

        assertThat(responses).hasSize(2);
        assertThat(responses)
                .extracting(PendingEmergencyRequestResponse::requestId)
                .containsExactly(1L, 2L);
        verify(hospitalResponseRepository).findAllByHospitalIdAndStatus(
                100L,
                HospitalResponseStatus.PENDING
        );
    }

    @Test
    void getPendingRequestsByHospitalExcludesAcceptedAndRejectedResponses() {
        given(hospitalResponseRepository.findAllByHospitalIdAndStatus(100L, HospitalResponseStatus.PENDING))
                .willReturn(List.of(hospitalResponseEntity(1L, 100L)));
        given(emergencyRequestRepository.findAllByIdIn(List.of(1L)))
                .willReturn(List.of(emergencyRequest(1L, "Chest pain and shortness of breath")));

        List<PendingEmergencyRequestResponse> responses =
                emergencyRequestService.getPendingRequestsByHospital(100L);

        assertThat(responses)
                .extracting(PendingEmergencyRequestResponse::requestId)
                .containsExactly(1L);
        verify(hospitalResponseRepository).findAllByHospitalIdAndStatus(
                100L,
                HospitalResponseStatus.PENDING
        );
    }

    @Test
    void getPendingRequestsByHospitalReturnsEmptyListWhenRequestsDoNotExist() {
        given(hospitalResponseRepository.findAllByHospitalIdAndStatus(100L, HospitalResponseStatus.PENDING))
                .willReturn(List.of());

        List<PendingEmergencyRequestResponse> responses =
                emergencyRequestService.getPendingRequestsByHospital(100L);

        assertThat(responses).isEmpty();
        verify(emergencyRequestRepository, never()).findAllByIdIn(any());
    }

    @Test
    void getEmergencyRequestDetailSucceedsForAdmin() {
        EmergencyRequest emergencyRequest = emergencyRequest(1L, "Chest pain and shortness of breath");
        given(emergencyRequestRepository.findById(1L)).willReturn(Optional.of(emergencyRequest));
        given(hospitalResponseRepository.findByEmergencyRequestId(1L))
                .willReturn(List.of(
                        hospitalResponseEntity(1L, 100L),
                        hospitalResponseEntity(1L, 200L)
                ));

        EmergencyRequestDetailResponse response = emergencyRequestService.getEmergencyRequestDetail(1L, 999L, "ADMIN");

        assertThat(response.requestId()).isEqualTo(1L);
        assertThat(response.paramedicId()).isEqualTo(10L);
        assertThat(response.patientCondition()).isEqualTo("Chest pain and shortness of breath");
        assertThat(response.patientGender()).isEqualTo(PatientGender.MALE);
        assertThat(response.patientAgeGroup()).isEqualTo("60s");
        assertThat(response.severityLevel()).isEqualTo(SeverityLevel.CRITICAL);
        assertThat(response.latitude()).isEqualTo(37.5665);
        assertThat(response.longitude()).isEqualTo(126.9780);
        assertThat(response.status()).isEqualTo(EmergencyRequestStatus.BROADCASTED);
        assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2026, 5, 10, 12, 0));
        assertThat(response.updatedAt()).isEqualTo(LocalDateTime.of(2026, 5, 10, 12, 0));
        assertThat(response.expiresAt()).isNotNull();
    }

    @Test
    void getEmergencyRequestDetailSucceedsForOwnerParamedic() {
        EmergencyRequest emergencyRequest = emergencyRequest(1L, "Chest pain and shortness of breath");
        given(emergencyRequestRepository.findById(1L)).willReturn(Optional.of(emergencyRequest));
        given(hospitalResponseRepository.findByEmergencyRequestId(1L))
                .willReturn(List.of(
                        hospitalResponseEntity(1L, 100L),
                        hospitalResponseEntity(1L, 200L)
                ));

        EmergencyRequestDetailResponse response = emergencyRequestService.getEmergencyRequestDetail(1L, 10L, "PARAMEDIC");

        assertThat(response.hospitalResponses()).hasSize(2);
        assertThat(response.hospitalResponses())
                .extracting(EmergencyRequestDetailResponse.HospitalResponseDetail::hospitalId)
                .containsExactly(100L, 200L);
        assertThat(response.hospitalResponses())
                .extracting(EmergencyRequestDetailResponse.HospitalResponseDetail::status)
                .containsOnly(HospitalResponseStatus.PENDING);
        assertThat(response.hospitalResponses())
                .extracting(EmergencyRequestDetailResponse.HospitalResponseDetail::createdAt)
                .containsOnly(LocalDateTime.of(2026, 5, 10, 12, 0));
    }

    @Test
    void getEmergencyRequestDetailFailsWhenParamedicIsNotOwner() {
        EmergencyRequest emergencyRequest = emergencyRequest(1L, "Chest pain and shortness of breath");
        given(emergencyRequestRepository.findById(1L)).willReturn(Optional.of(emergencyRequest));

        assertThatThrownBy(() -> emergencyRequestService.getEmergencyRequestDetail(1L, 20L, "PARAMEDIC"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("해당 응급 요청을 조회할 권한이 없습니다.");

        verify(hospitalResponseRepository, never()).findByEmergencyRequestId(any());
    }

    @Test
    void getEmergencyRequestDetailSucceedsForCandidateHospital() {
        EmergencyRequest emergencyRequest = emergencyRequest(1L, "Chest pain and shortness of breath");
        given(emergencyRequestRepository.findById(1L)).willReturn(Optional.of(emergencyRequest));
        given(hospitalResponseRepository.existsByEmergencyRequestIdAndHospitalId(1L, 100L))
                .willReturn(true);
        given(hospitalResponseRepository.findByEmergencyRequestId(1L))
                .willReturn(List.of(hospitalResponseEntity(1L, 100L)));

        EmergencyRequestDetailResponse response = emergencyRequestService.getEmergencyRequestDetail(1L, 100L, "HOSPITAL");

        assertThat(response.requestId()).isEqualTo(1L);
        assertThat(response.hospitalResponses())
                .extracting(EmergencyRequestDetailResponse.HospitalResponseDetail::hospitalId)
                .containsExactly(100L);
    }

    @Test
    void getEmergencyRequestDetailFailsWhenHospitalIsNotCandidate() {
        EmergencyRequest emergencyRequest = emergencyRequest(1L, "Chest pain and shortness of breath");
        given(emergencyRequestRepository.findById(1L)).willReturn(Optional.of(emergencyRequest));
        given(hospitalResponseRepository.existsByEmergencyRequestIdAndHospitalId(1L, 999L))
                .willReturn(false);

        assertThatThrownBy(() -> emergencyRequestService.getEmergencyRequestDetail(1L, 999L, "HOSPITAL"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("해당 응급 요청을 조회할 권한이 없습니다.");

        verify(hospitalResponseRepository, never()).findByEmergencyRequestId(any());
    }

    @Test
    void getEmergencyRequestDetailFailsForUnknownRole() {
        EmergencyRequest emergencyRequest = emergencyRequest(1L, "Chest pain and shortness of breath");
        given(emergencyRequestRepository.findById(1L)).willReturn(Optional.of(emergencyRequest));

        assertThatThrownBy(() -> emergencyRequestService.getEmergencyRequestDetail(1L, 10L, "UNKNOWN"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("해당 응급 요청을 조회할 권한이 없습니다.");

        verify(hospitalResponseRepository, never()).findByEmergencyRequestId(any());
    }

    @Test
    void getEmergencyRequestDetailThrowsWhenRequestDoesNotExist() {
        given(emergencyRequestRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> emergencyRequestService.getEmergencyRequestDetail(999L, 10L, "ADMIN"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("해당 응급 요청을 찾을 수 없습니다.");

        verify(hospitalResponseRepository, never()).findByEmergencyRequestId(any());
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

    private EmergencyRequest emergencyRequest(Long id, String patientCondition) {
        EmergencyRequest emergencyRequest = EmergencyRequest.create(
                10L,
                patientCondition,
                PatientGender.MALE,
                "60s",
                SeverityLevel.CRITICAL,
                37.5665,
                126.9780
        );
        emergencyRequest.broadcast();
        return emergencyRequestWithId(emergencyRequest, id);
    }

    private EmergencyRequest broadcastedEmergencyRequest(Long id) {
        EmergencyRequest emergencyRequest = EmergencyRequest.create(
                10L,
                "Chest pain",
                PatientGender.MALE,
                "60s",
                SeverityLevel.CRITICAL,
                37.5665,
                126.9780
        );
        ReflectionTestUtils.setField(emergencyRequest, "id", id);
        emergencyRequest.broadcast();
        return emergencyRequest;
    }

    private HospitalResponse hospitalResponseEntity(Long emergencyRequestId, Long hospitalId) {
        HospitalResponse hospitalResponse = HospitalResponse.pending(emergencyRequestId, hospitalId);
        ReflectionTestUtils.setField(hospitalResponse, "id", emergencyRequestId);
        ReflectionTestUtils.setField(
                hospitalResponse,
                "createdAt",
                LocalDateTime.of(2026, 5, 10, 12, 0)
        );
        return hospitalResponse;
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
