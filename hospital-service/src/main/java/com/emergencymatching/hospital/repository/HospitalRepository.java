package com.emergencymatching.hospital.repository;

import com.emergencymatching.hospital.domain.Hospital;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HospitalRepository extends JpaRepository<Hospital, Long> {

    @Query(value = """
            SELECT
                h.id AS hospitalId,
                h.name AS name,
                h.address AS address,
                h.latitude AS latitude,
                h.longitude AS longitude,
                h.is_available AS isAvailable,
                ST_Distance(
                    ST_SetSRID(ST_MakePoint(h.longitude, h.latitude), 4326)::geography,
                    ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography
                ) / 1000.0 AS distanceKm
            FROM hospitals h
            WHERE h.is_available = true
              AND ST_DWithin(
                  ST_SetSRID(ST_MakePoint(h.longitude, h.latitude), 4326)::geography,
                  ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                  :radiusMeters
              )
            ORDER BY distanceKm ASC
            """, nativeQuery = true)
    List<NearbyHospitalProjection> findNearbyAvailableHospitals(
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("radiusMeters") Double radiusMeters
    );
}
