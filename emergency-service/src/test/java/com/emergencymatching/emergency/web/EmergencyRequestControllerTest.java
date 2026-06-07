package com.emergencymatching.emergency.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.emergencymatching.emergency.domain.EmergencyRequestStatus;
import com.emergencymatching.emergency.domain.PatientGender;
import com.emergencymatching.emergency.domain.SeverityLevel;
import com.emergencymatching.emergency.service.EmergencyRequestService;
import com.emergencymatching.emergency.web.dto.CandidateHospitalResponse;
import com.emergencymatching.emergency.web.dto.CreateEmergencyRequestRequest;
import com.emergencymatching.emergency.web.dto.EmergencyRequestResponse;
import com.emergencymatching.emergency.web.dto.PendingEmergencyRequestResponse;
import java.time.LocalDateTime;
import java.util.List;
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
                        .header("X-User-Id", "10")
                        .header("X-User-Role", "PARAMEDIC")
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
                .andExpect(jsonPath("$.status").value("BROADCASTED"))
                .andExpect(jsonPath("$.severityLevel").value("CRITICAL"))
                .andExpect(jsonPath("$.patientGender").value("MALE"))
                .andExpect(jsonPath("$.candidateHospitals[0].hospitalId").value(1))
                .andExpect(jsonPath("$.candidateHospitals[0].name").value("Seoul Emergency Hospital"))
                .andExpect(jsonPath("$.candidateHospitals[0].distanceKm").value(1.2))
                .andExpect(jsonPath("$.candidateHospitals[0].isAvailable").value(true));
    }

    @Test
    void createEmergencyRequestReturnsBadRequestForInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/emergency-requests")
                        .header("X-User-Id", "10")
                        .header("X-User-Role", "PARAMEDIC")
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

    @Test
    void getPendingRequestsByHospitalReturnsOkResponse() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 5, 10, 12, 0);
        given(emergencyRequestService.getPendingRequestsByHospital(100L))
                .willReturn(List.of(new PendingEmergencyRequestResponse(
                        1L,
                        "Chest pain and shortness of breath",
                        PatientGender.MALE,
                        "60s",
                        SeverityLevel.CRITICAL,
                        37.5665,
                        126.9780,
                        EmergencyRequestStatus.BROADCASTED,
                        now,
                        now.plusMinutes(10)
                )));

        mockMvc.perform(get("/api/emergency-requests/hospitals/{hospitalId}/pending", 100L)
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "HOSPITAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requestId").value(1))
                .andExpect(jsonPath("$[0].patientCondition").value("Chest pain and shortness of breath"))
                .andExpect(jsonPath("$[0].patientGender").value("MALE"))
                .andExpect(jsonPath("$[0].patientAgeGroup").value("60s"))
                .andExpect(jsonPath("$[0].severityLevel").value("CRITICAL"))
                .andExpect(jsonPath("$[0].latitude").value(37.5665))
                .andExpect(jsonPath("$[0].longitude").value(126.9780))
                .andExpect(jsonPath("$[0].status").value("BROADCASTED"));
    }

    @Test
    void createEmergencyRequestFailsWhenNotParamedic() throws Exception {
        mockMvc.perform(post("/api/emergency-requests")
                        .header("X-User-Id", "10")
                        .header("X-User-Role", "HOSPITAL")
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
                .andExpect(status().isBadRequest());
    }

    @Test
    void acceptEmergencyRequestSucceedsWithValidHeaders() throws Exception {
        mockMvc.perform(post("/api/emergency-requests/1/accept")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "HOSPITAL")
                        .param("hospitalId", "100"))
                .andExpect(status().isOk());
    }

    @Test
    void acceptEmergencyRequestFailsForDifferentHospital() throws Exception {
        mockMvc.perform(post("/api/emergency-requests/1/accept")
                        .header("X-User-Id", "100")
                        .header("X-User-Role", "HOSPITAL")
                        .param("hospitalId", "200"))
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
                EmergencyRequestStatus.BROADCASTED,
                null,
                now,
                now,
                now.plusMinutes(10),
                0L,
                List.of(new CandidateHospitalResponse(
                        1L,
                        "Seoul Emergency Hospital",
                        "123 Seoul-ro",
                        37.5665,
                        126.9780,
                        1.2,
                        true
                ))
        );
    }
}
