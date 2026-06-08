package com.emergencymatching.emergency.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;

@Entity
@Table(name = "emergency_requests")
public class EmergencyRequest {

    private static final long DEFAULT_EXPIRATION_MINUTES = 10L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long paramedicId;

    @Column(nullable = false, length = 1000)
    private String patientCondition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PatientGender patientGender;

    @Column(nullable = false, length = 50)
    private String patientAgeGroup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeverityLevel severityLevel;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmergencyRequestStatus status;

    private Long acceptedHospitalId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Version
    private Long version;

    protected EmergencyRequest() {
    }

    private EmergencyRequest(
            Long paramedicId,
            String patientCondition,
            PatientGender patientGender,
            String patientAgeGroup,
            SeverityLevel severityLevel,
            Double latitude,
            Double longitude
    ) {
        this.paramedicId = paramedicId;
        this.patientCondition = patientCondition;
        this.patientGender = patientGender;
        this.patientAgeGroup = patientAgeGroup;
        this.severityLevel = severityLevel;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = EmergencyRequestStatus.REQUESTED;
        this.expiresAt = LocalDateTime.now().plusMinutes(DEFAULT_EXPIRATION_MINUTES);
    }

    public static EmergencyRequest create(
            Long paramedicId,
            String patientCondition,
            PatientGender patientGender,
            String patientAgeGroup,
            SeverityLevel severityLevel,
            Double latitude,
            Double longitude
    ) {
        return new EmergencyRequest(
                paramedicId,
                patientCondition,
                patientGender,
                patientAgeGroup,
                severityLevel,
                latitude,
                longitude
        );
    }

    public void broadcast() {
        if (this.status != EmergencyRequestStatus.REQUESTED) {
            throw new IllegalStateException("REQUESTED 상태의 요청만 전송(BROADCAST)할 수 있습니다.");
        }
        this.status = EmergencyRequestStatus.BROADCASTED;
    }

    public void accept(Long hospitalId) {
        if (this.status != EmergencyRequestStatus.BROADCASTED) {
            throw new IllegalStateException("전송 완료(BROADCASTED) 상태의 요청만 수락할 수 있습니다.");
        }
        if (LocalDateTime.now().isAfter(this.expiresAt)) {
            throw new IllegalStateException("만료된 요청은 수락할 수 없습니다.");
        }
        this.status = EmergencyRequestStatus.ACCEPTED;
        this.acceptedHospitalId = hospitalId;
    }

    public void expire() {
        if (this.status != EmergencyRequestStatus.REQUESTED
                && this.status != EmergencyRequestStatus.BROADCASTED) {
            throw new IllegalStateException("REQUESTED 또는 BROADCASTED 상태의 요청만 만료 처리할 수 있습니다.");
        }
        this.status = EmergencyRequestStatus.EXPIRED;
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getParamedicId() {
        return paramedicId;
    }

    public String getPatientCondition() {
        return patientCondition;
    }

    public PatientGender getPatientGender() {
        return patientGender;
    }

    public String getPatientAgeGroup() {
        return patientAgeGroup;
    }

    public SeverityLevel getSeverityLevel() {
        return severityLevel;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public EmergencyRequestStatus getStatus() {
        return status;
    }

    public Long getAcceptedHospitalId() {
        return acceptedHospitalId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public Long getVersion() {
        return version;
    }
}
