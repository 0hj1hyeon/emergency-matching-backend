package com.emergencymatching.hospital.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.emergencymatching.hospital.domain.Hospital;
import com.emergencymatching.hospital.exception.HospitalNotFoundException;
import com.emergencymatching.hospital.repository.NearbyHospitalProjection;
import com.emergencymatching.hospital.repository.HospitalRepository;
import com.emergencymatching.hospital.web.dto.CreateHospitalRequest;
import com.emergencymatching.hospital.web.dto.HospitalResponse;
import com.emergencymatching.hospital.web.dto.NearbyHospitalResponse;
import com.emergencymatching.hospital.web.dto.NearbyHospitalSearchRequest;
import com.emergencymatching.hospital.web.dto.UpdateHospitalAvailabilityRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class HospitalServiceTest {

    @Mock
    private HospitalRepository hospitalRepository;

    @InjectMocks
    private HospitalService hospitalService;

    @Test
    void createHospitalSucceeds() {
        CreateHospitalRequest request = new CreateHospitalRequest(
                10L,
                "Seoul Emergency Hospital",
                "123 Seoul-ro",
                37.5665,
                126.9780,
                true
        );

        given(hospitalRepository.save(any(Hospital.class)))
                .willAnswer(invocation -> hospitalWithId(invocation.getArgument(0), 1L));

        HospitalResponse response = hospitalService.createHospital(request);

        ArgumentCaptor<Hospital> captor = ArgumentCaptor.forClass(Hospital.class);
        verify(hospitalRepository).save(captor.capture());

        Hospital savedHospital = captor.getValue();
        assertThat(savedHospital.getMemberId()).isEqualTo(10L);
        assertThat(savedHospital.getName()).isEqualTo("Seoul Emergency Hospital");
        assertThat(savedHospital.getAddress()).isEqualTo("123 Seoul-ro");
        assertThat(savedHospital.getLatitude()).isEqualTo(37.5665);
        assertThat(savedHospital.getLongitude()).isEqualTo(126.9780);
        assertThat(savedHospital.getAvailable()).isTrue();

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Seoul Emergency Hospital");
        assertThat(response.isAvailable()).isTrue();
    }

    @Test
    void getHospitalsReturnsList() {
        Hospital firstHospital = hospital(
                1L,
                10L,
                "Seoul Emergency Hospital",
                "123 Seoul-ro",
                37.5665,
                126.9780,
                true
        );
        Hospital secondHospital = hospital(
                2L,
                20L,
                "Busan Emergency Hospital",
                "456 Busan-ro",
                35.1796,
                129.0756,
                false
        );
        given(hospitalRepository.findAll()).willReturn(List.of(firstHospital, secondHospital));

        List<HospitalResponse> responses = hospitalService.getHospitals();

        assertThat(responses).hasSize(2);
        assertThat(responses)
                .extracting(HospitalResponse::name)
                .containsExactly("Seoul Emergency Hospital", "Busan Emergency Hospital");
    }

    @Test
    void getHospitalSucceeds() {
        Hospital hospital = hospital(
                1L,
                10L,
                "Seoul Emergency Hospital",
                "123 Seoul-ro",
                37.5665,
                126.9780,
                true
        );
        given(hospitalRepository.findById(1L)).willReturn(Optional.of(hospital));

        HospitalResponse response = hospitalService.getHospital(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.memberId()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("Seoul Emergency Hospital");
        assertThat(response.isAvailable()).isTrue();
    }

    @Test
    void getHospitalThrowsExceptionWhenHospitalDoesNotExist() {
        given(hospitalRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> hospitalService.getHospital(999L))
                .isInstanceOf(HospitalNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void updateAvailabilitySucceeds() {
        Hospital hospital = hospital(
                1L,
                10L,
                "Seoul Emergency Hospital",
                "123 Seoul-ro",
                37.5665,
                126.9780,
                true
        );
        given(hospitalRepository.findById(1L)).willReturn(Optional.of(hospital));
        UpdateHospitalAvailabilityRequest request = new UpdateHospitalAvailabilityRequest(false);

        HospitalResponse response = hospitalService.updateAvailability(1L, request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.isAvailable()).isFalse();
        assertThat(hospital.getAvailable()).isFalse();
    }

    @Test
    void getNearbyHospitalsReturnsHospitalsWithinRadius() {
        NearbyHospitalSearchRequest request = new NearbyHospitalSearchRequest(
                37.5665,
                126.9780,
                5.0
        );
        given(hospitalRepository.findNearbyAvailableHospitals(37.5665, 126.9780, 5000.0))
                .willReturn(List.of(
                        nearbyHospital(1L, "Seoul Emergency Hospital", 1.2, true),
                        nearbyHospital(2L, "Gangnam Emergency Hospital", 3.4, true)
                ));

        List<NearbyHospitalResponse> responses = hospitalService.getNearbyHospitals(request);

        assertThat(responses).hasSize(2);
        assertThat(responses)
                .extracting(NearbyHospitalResponse::hospitalId)
                .containsExactly(1L, 2L);
    }

    @Test
    void getNearbyHospitalsReturnsOnlyAvailableHospitals() {
        NearbyHospitalSearchRequest request = new NearbyHospitalSearchRequest(
                37.5665,
                126.9780,
                5.0
        );
        given(hospitalRepository.findNearbyAvailableHospitals(37.5665, 126.9780, 5000.0))
                .willReturn(List.of(
                        nearbyHospital(1L, "Seoul Emergency Hospital", 1.2, true),
                        nearbyHospital(2L, "Gangnam Emergency Hospital", 3.4, true)
                ));

        List<NearbyHospitalResponse> responses = hospitalService.getNearbyHospitals(request);

        assertThat(responses)
                .extracting(NearbyHospitalResponse::isAvailable)
                .containsOnly(true);
    }

    @Test
    void getNearbyHospitalsPreservesDistanceOrder() {
        NearbyHospitalSearchRequest request = new NearbyHospitalSearchRequest(
                37.5665,
                126.9780,
                5.0
        );
        given(hospitalRepository.findNearbyAvailableHospitals(37.5665, 126.9780, 5000.0))
                .willReturn(List.of(
                        nearbyHospital(1L, "Seoul Emergency Hospital", 1.2, true),
                        nearbyHospital(2L, "Gangnam Emergency Hospital", 3.4, true)
                ));

        List<NearbyHospitalResponse> responses = hospitalService.getNearbyHospitals(request);

        assertThat(responses)
                .extracting(NearbyHospitalResponse::distanceKm)
                .containsExactly(1.2, 3.4);
    }

    @Test
    void getNearbyHospitalsConvertsRadiusKmToMeters() {
        NearbyHospitalSearchRequest request = new NearbyHospitalSearchRequest(
                37.5665,
                126.9780,
                7.5
        );
        given(hospitalRepository.findNearbyAvailableHospitals(any(), any(), any()))
                .willReturn(List.of());

        hospitalService.getNearbyHospitals(request);

        verify(hospitalRepository).findNearbyAvailableHospitals(
                eq(37.5665),
                eq(126.9780),
                eq(7500.0)
        );
    }

    private Hospital hospital(
            Long id,
            Long memberId,
            String name,
            String address,
            Double latitude,
            Double longitude,
            Boolean isAvailable
    ) {
        Hospital hospital = Hospital.create(memberId, name, address, latitude, longitude, isAvailable);
        return hospitalWithId(hospital, id);
    }

    private Hospital hospitalWithId(Hospital hospital, Long id) {
        LocalDateTime now = LocalDateTime.of(2026, 5, 9, 12, 0);
        ReflectionTestUtils.setField(hospital, "id", id);
        ReflectionTestUtils.setField(hospital, "createdAt", now);
        ReflectionTestUtils.setField(hospital, "updatedAt", now);
        return hospital;
    }

    private NearbyHospitalProjection nearbyHospital(
            Long hospitalId,
            String name,
            Double distanceKm,
            Boolean isAvailable
    ) {
        return new TestNearbyHospitalProjection(
                hospitalId,
                name,
                "123 Seoul-ro",
                37.5665,
                126.9780,
                distanceKm,
                isAvailable
        );
    }

    private record TestNearbyHospitalProjection(
            Long hospitalId,
            String name,
            String address,
            Double latitude,
            Double longitude,
            Double distanceKm,
            Boolean isAvailable
    ) implements NearbyHospitalProjection {

        @Override
        public Long getHospitalId() {
            return hospitalId;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getAddress() {
            return address;
        }

        @Override
        public Double getLatitude() {
            return latitude;
        }

        @Override
        public Double getLongitude() {
            return longitude;
        }

        @Override
        public Double getDistanceKm() {
            return distanceKm;
        }

        @Override
        public Boolean getIsAvailable() {
            return isAvailable;
        }
    }
}
