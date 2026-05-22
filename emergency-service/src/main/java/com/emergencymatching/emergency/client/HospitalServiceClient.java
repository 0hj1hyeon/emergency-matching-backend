package com.emergencymatching.emergency.client;

import com.emergencymatching.emergency.client.dto.HospitalResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Hospital Service와 통신하기 위한 Feign Client 단축번호 인터페이스입니다.
 * 
 * `@FeignClient` 어노테이션은 이 인터페이스를 기반으로 스프링이 알아서 HTTP 호출 구현체를 채우도록 만듭니다.
 * - name: 서비스 식별자
 * - url: 호출할 호스트 주소 (기본값: 로컬 hospital-service 주소인 http://localhost:8082)
 */
@FeignClient(name = "hospital-service", url = "${services.hospital-service.url:http://localhost:8082}")
public interface HospitalServiceClient {

    /**
     * 특정 위도, 경도 기준 반경(radius) 내의 수용 가능한 병원 목록을 가져옵니다.
     * 
     * @param lat 환자의 위도
     * @param lng 환자의 경도
     * @param radius 검색 반경 (km)
     * @return 근처 병원 목록 DTO 리스트
     */
    @GetMapping("/api/hospitals/nearby")
    List<HospitalResponseDto> getNearbyHospitals(
            @RequestParam("lat") Double lat,
            @RequestParam("lng") Double lng,
            @RequestParam("radius") Double radius
    );
}
