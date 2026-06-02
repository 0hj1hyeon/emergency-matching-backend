package com.emergencymatching.emergency.repository;

import com.emergencymatching.emergency.domain.HospitalResponse;
import com.emergencymatching.emergency.domain.HospitalResponseStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalResponseRepository extends JpaRepository<HospitalResponse, Long> {

    List<HospitalResponse> findAllByHospitalIdAndStatus(
            Long hospitalId,
            HospitalResponseStatus status
    );
}
