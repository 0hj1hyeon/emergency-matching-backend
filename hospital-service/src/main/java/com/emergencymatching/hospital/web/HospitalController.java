package com.emergencymatching.hospital.web;

import com.emergencymatching.hospital.service.HospitalService;
import com.emergencymatching.hospital.web.dto.CreateHospitalRequest;
import com.emergencymatching.hospital.web.dto.HospitalResponse;
import com.emergencymatching.hospital.web.dto.NearbyHospitalResponse;
import com.emergencymatching.hospital.web.dto.NearbyHospitalSearchRequest;
import com.emergencymatching.hospital.web.dto.UpdateHospitalAvailabilityRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/hospitals")
public class HospitalController {

    private final HospitalService hospitalService;

    public HospitalController(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    @PostMapping
    public ResponseEntity<HospitalResponse> createHospital(
            @Valid @RequestBody CreateHospitalRequest request
    ) {
        HospitalResponse response = hospitalService.createHospital(request);

        return ResponseEntity.created(URI.create("/api/hospitals/" + response.id()))
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<HospitalResponse>> getHospitals() {
        return ResponseEntity.ok(hospitalService.getHospitals());
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<NearbyHospitalResponse>> getNearbyHospitals(
            @Valid NearbyHospitalSearchRequest request
    ) {
        return ResponseEntity.ok(hospitalService.getNearbyHospitals(request));
    }

    @GetMapping("/{hospitalId}")
    public ResponseEntity<HospitalResponse> getHospital(@PathVariable Long hospitalId) {
        return ResponseEntity.ok(hospitalService.getHospital(hospitalId));
    }

    @PatchMapping("/{hospitalId}/availability")
    public ResponseEntity<HospitalResponse> updateAvailability(
            @PathVariable Long hospitalId,
            @Valid @RequestBody UpdateHospitalAvailabilityRequest request
    ) {
        return ResponseEntity.ok(hospitalService.updateAvailability(hospitalId, request));
    }
}
