package com.emergencymatching.hospital.service;

import com.emergencymatching.hospital.domain.Hospital;
import com.emergencymatching.hospital.exception.HospitalNotFoundException;
import com.emergencymatching.hospital.repository.HospitalRepository;
import com.emergencymatching.hospital.web.dto.CreateHospitalRequest;
import com.emergencymatching.hospital.web.dto.HospitalResponse;
import com.emergencymatching.hospital.web.dto.UpdateHospitalAvailabilityRequest;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class HospitalService {

    private final HospitalRepository hospitalRepository;

    public HospitalService(HospitalRepository hospitalRepository) {
        this.hospitalRepository = hospitalRepository;
    }

    @Transactional
    public HospitalResponse createHospital(CreateHospitalRequest request) {
        Hospital hospital = Hospital.create(
                request.memberId(),
                request.name(),
                request.address(),
                request.latitude(),
                request.longitude(),
                request.isAvailable()
        );

        return HospitalResponse.from(hospitalRepository.save(hospital));
    }

    public List<HospitalResponse> getHospitals() {
        return hospitalRepository.findAll()
                .stream()
                .map(HospitalResponse::from)
                .toList();
    }

    public HospitalResponse getHospital(Long hospitalId) {
        return HospitalResponse.from(findHospital(hospitalId));
    }

    @Transactional
    public HospitalResponse updateAvailability(
            Long hospitalId,
            UpdateHospitalAvailabilityRequest request
    ) {
        Hospital hospital = findHospital(hospitalId);
        hospital.updateAvailability(request.isAvailable());

        return HospitalResponse.from(hospital);
    }

    private Hospital findHospital(Long hospitalId) {
        return hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new HospitalNotFoundException(hospitalId));
    }
}
