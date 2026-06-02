package com.emergencymatching.emergency.repository;

import com.emergencymatching.emergency.domain.EmergencyRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmergencyRequestRepository extends JpaRepository<EmergencyRequest, Long> {

    List<EmergencyRequest> findAllByIdIn(List<Long> ids);
}
