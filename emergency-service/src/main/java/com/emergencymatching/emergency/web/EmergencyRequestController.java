package com.emergencymatching.emergency.web;

import com.emergencymatching.emergency.service.EmergencyRequestService;
import com.emergencymatching.emergency.web.dto.CreateEmergencyRequestRequest;
import com.emergencymatching.emergency.web.dto.EmergencyRequestResponse;
import com.emergencymatching.emergency.web.dto.PendingEmergencyRequestResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/emergency-requests")
public class EmergencyRequestController {

    private final EmergencyRequestService emergencyRequestService;

    public EmergencyRequestController(EmergencyRequestService emergencyRequestService) {
        this.emergencyRequestService = emergencyRequestService;
    }

    @PostMapping
    public ResponseEntity<EmergencyRequestResponse> createEmergencyRequest(
            @Valid @RequestBody CreateEmergencyRequestRequest request
    ) {
        EmergencyRequestResponse response = emergencyRequestService.createEmergencyRequest(request);

        return ResponseEntity.created(URI.create("/api/emergency-requests/" + response.id()))
                .body(response);
    }

    @GetMapping("/hospitals/{hospitalId}/pending")
    public ResponseEntity<List<PendingEmergencyRequestResponse>> getPendingRequestsByHospital(
            @PathVariable Long hospitalId
    ) {
        return ResponseEntity.ok(emergencyRequestService.getPendingRequestsByHospital(hospitalId));
    }
}
