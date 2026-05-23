package com.emergencymatching.emergency.client;

import com.emergencymatching.emergency.client.dto.HospitalResponseDto;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "hospital-service", url = "${services.hospital-service.url:http://localhost:8082}")
public interface HospitalClient {

    @GetMapping("/api/hospitals/nearby")
    List<HospitalResponseDto> getNearbyHospitals(
            @RequestParam("lat") Double lat,
            @RequestParam("lng") Double lng,
            @RequestParam("radiusKm") Double radiusKm
    );
}
