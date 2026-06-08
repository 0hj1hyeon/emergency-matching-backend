package com.emergencymatching.emergency.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "hospital_responses")
public class HospitalResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long emergencyRequestId;

    @Column(nullable = false)
    private Long hospitalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HospitalResponseStatus status;

    private LocalDateTime respondedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected HospitalResponse() {
    }

    private HospitalResponse(Long emergencyRequestId, Long hospitalId) {
        this.emergencyRequestId = emergencyRequestId;
        this.hospitalId = hospitalId;
        this.status = HospitalResponseStatus.PENDING;
    }

    public static HospitalResponse pending(Long emergencyRequestId, Long hospitalId) {
        return new HospitalResponse(emergencyRequestId, hospitalId);
    }

    public void accept() {
        if (this.status != HospitalResponseStatus.PENDING) {
            throw new IllegalStateException("대기 상태(PENDING)의 병원 응답만 수락할 수 있습니다.");
        }
        this.status = HospitalResponseStatus.ACCEPTED;
        this.respondedAt = LocalDateTime.now();
    }

    public void reject() {
        if (this.status != HospitalResponseStatus.PENDING) {
            throw new IllegalStateException("대기 상태(PENDING)의 병원 응답만 거절할 수 있습니다.");
        }
        this.status = HospitalResponseStatus.REJECTED;
        this.respondedAt = LocalDateTime.now();
    }

    public void timeout() {
        if (this.status != HospitalResponseStatus.PENDING) {
            throw new IllegalStateException("대기 상태(PENDING)의 병원 응답만 타임아웃 처리할 수 있습니다.");
        }
        this.status = HospitalResponseStatus.TIMEOUT;
        this.respondedAt = LocalDateTime.now();
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getEmergencyRequestId() {
        return emergencyRequestId;
    }

    public Long getHospitalId() {
        return hospitalId;
    }

    public HospitalResponseStatus getStatus() {
        return status;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
