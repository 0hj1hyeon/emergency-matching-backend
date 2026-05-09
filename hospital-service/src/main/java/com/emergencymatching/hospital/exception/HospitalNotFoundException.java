package com.emergencymatching.hospital.exception;

public class HospitalNotFoundException extends RuntimeException {

    public HospitalNotFoundException(Long hospitalId) {
        super("Hospital not found. id=" + hospitalId);
    }
}
