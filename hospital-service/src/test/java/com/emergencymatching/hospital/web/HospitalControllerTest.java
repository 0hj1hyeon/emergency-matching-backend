package com.emergencymatching.hospital.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emergencymatching.hospital.service.HospitalService;
import com.emergencymatching.hospital.web.dto.CreateHospitalRequest;
import com.emergencymatching.hospital.web.dto.HospitalResponse;
import com.emergencymatching.hospital.web.dto.NearbyHospitalResponse;
import com.emergencymatching.hospital.web.dto.NearbyHospitalSearchRequest;
import com.emergencymatching.hospital.web.dto.UpdateHospitalAvailabilityRequest;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HospitalController.class)
class HospitalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HospitalService hospitalService;

    @Test
    void createHospitalReturnsCreatedResponse() throws Exception {
        HospitalResponse response = hospitalResponse(1L, true);
        given(hospitalService.createHospital(any(CreateHospitalRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/hospitals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "memberId": 10,
                                  "name": "Seoul Emergency Hospital",
                                  "address": "123 Seoul-ro",
                                  "latitude": 37.5665,
                                  "longitude": 126.9780,
                                  "isAvailable": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/hospitals/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.memberId").value(10))
                .andExpect(jsonPath("$.name").value("Seoul Emergency Hospital"))
                .andExpect(jsonPath("$.isAvailable").value(true));
    }

    @Test
    void getHospitalsReturnsOkResponse() throws Exception {
        given(hospitalService.getHospitals())
                .willReturn(List.of(hospitalResponse(1L, true), hospitalResponse(2L, false)));

        mockMvc.perform(get("/api/hospitals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Seoul Emergency Hospital"))
                .andExpect(jsonPath("$[0].isAvailable").value(true))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].isAvailable").value(false));
    }

    @Test
    void getNearbyHospitalsReturnsOkResponse() throws Exception {
        given(hospitalService.getNearbyHospitals(any(NearbyHospitalSearchRequest.class)))
                .willReturn(List.of(nearbyHospitalResponse()));

        mockMvc.perform(get("/api/hospitals/nearby")
                        .param("lat", "37.5665")
                        .param("lng", "126.9780")
                        .param("radiusKm", "5.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].hospitalId").value(1))
                .andExpect(jsonPath("$[0].name").value("Seoul Emergency Hospital"))
                .andExpect(jsonPath("$[0].address").value("123 Seoul-ro"))
                .andExpect(jsonPath("$[0].latitude").value(37.5665))
                .andExpect(jsonPath("$[0].longitude").value(126.9780))
                .andExpect(jsonPath("$[0].distanceKm").value(1.2))
                .andExpect(jsonPath("$[0].isAvailable").value(true));
    }

    @Test
    void getNearbyHospitalsReturnsBadRequestForInvalidParams() throws Exception {
        mockMvc.perform(get("/api/hospitals/nearby")
                        .param("lat", "91.0")
                        .param("lng", "181.0")
                        .param("radiusKm", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getHospitalReturnsOkResponse() throws Exception {
        given(hospitalService.getHospital(1L)).willReturn(hospitalResponse(1L, true));

        mockMvc.perform(get("/api/hospitals/{hospitalId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.memberId").value(10))
                .andExpect(jsonPath("$.name").value("Seoul Emergency Hospital"))
                .andExpect(jsonPath("$.isAvailable").value(true));
    }

    @Test
    void updateAvailabilityReturnsOkResponse() throws Exception {
        HospitalResponse response = hospitalResponse(1L, false);
        given(hospitalService.updateAvailability(
                eq(1L),
                any(UpdateHospitalAvailabilityRequest.class)
        )).willReturn(response);

        mockMvc.perform(patch("/api/hospitals/{hospitalId}/availability", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "isAvailable": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.isAvailable").value(false));
    }

    private HospitalResponse hospitalResponse(Long id, Boolean isAvailable) {
        LocalDateTime timestamp = LocalDateTime.of(2026, 5, 9, 12, 0);
        return new HospitalResponse(
                id,
                10L,
                "Seoul Emergency Hospital",
                "123 Seoul-ro",
                37.5665,
                126.9780,
                isAvailable,
                timestamp,
                timestamp
        );
    }

    private NearbyHospitalResponse nearbyHospitalResponse() {
        return new NearbyHospitalResponse(
                1L,
                "Seoul Emergency Hospital",
                "123 Seoul-ro",
                37.5665,
                126.9780,
                1.2,
                true
        );
    }
}
