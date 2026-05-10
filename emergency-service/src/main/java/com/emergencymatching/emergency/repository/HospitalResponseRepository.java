package com.emergencymatching.emergency.repository;

import com.emergencymatching.emergency.domain.HospitalResponse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HospitalResponseRepository extends JpaRepository<HospitalResponse, Long> {
}
