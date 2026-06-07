package com.emergencymatching.emergency.exception;

import java.time.LocalDateTime;

/**
 * API 호출 에러 발생 시 공통으로 클라이언트에 내려줄 에러 응답 DTO입니다.
 */
public record ErrorResponse(
        String code,
        String message,
        LocalDateTime timestamp
) {
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, LocalDateTime.now());
    }
}
