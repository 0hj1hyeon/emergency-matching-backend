package com.emergencymatching.emergency.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emergencymatching.emergency.domain.EmergencyRequestStatus;
import com.emergencymatching.emergency.domain.PatientGender;
import com.emergencymatching.emergency.domain.SeverityLevel;
import com.emergencymatching.emergency.service.EmergencyRequestService;
import com.emergencymatching.emergency.web.dto.CreateEmergencyRequestRequest;
import com.emergencymatching.emergency.web.dto.EmergencyRequestResponse;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EmergencyRequestController.class)
class EmergencyRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmergencyRequestService emergencyRequestService;

    @Test
    void createEmergencyRequestReturnsCreatedResponse() throws Exception {
        given(emergencyRequestService.createEmergencyRequest(any(CreateEmergencyRequestRequest.class)))
                .willReturn(emergencyRequestResponse());

        mockMvc.perform(post("/api/emergency-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paramedicId": 10,
                                  "patientCondition": "Chest pain and shortness of breath",
                                  "patientGender": "MALE",
                                  "patientAgeGroup": "60s",
                                  "severityLevel": "CRITICAL",
                                  "latitude": 37.5665,
                                  "longitude": 126.9780
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/emergency-requests/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.paramedicId").value(10))
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.severityLevel").value("CRITICAL"))
                .andExpect(jsonPath("$.patientGender").value("MALE"));
    }

    @Test
    void createEmergencyRequestReturnsBadRequestForInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/emergency-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paramedicId": null,
                                  "patientCondition": "",
                                  "patientGender": null,
                                  "patientAgeGroup": "",
                                  "severityLevel": null,
                                  "latitude": 91.0,
                                  "longitude": 181.0
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    private EmergencyRequestResponse emergencyRequestResponse() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 10, 12, 0);
        return new EmergencyRequestResponse(
                1L,
                10L,
                "Chest pain and shortness of breath",
                PatientGender.MALE,
                "60s",
                SeverityLevel.CRITICAL,
                37.5665,
                126.9780,
                EmergencyRequestStatus.REQUESTED,
                null,
                now,
                now,
                now.plusMinutes(10),
                0L
        );
    }
}
