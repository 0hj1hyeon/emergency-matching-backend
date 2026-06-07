package com.emergencymatching.notification.config;

import java.security.Principal;

/**
 * WebSocket STOMP 세션에 연결된 사용자의 인증 정보를 보관하는 Principal 구현체입니다.
 */
public class StompPrincipal implements Principal {

    private final String name; // 사용자 고유 식별자 (memberId String 값)
    private final String role; // 사용자 권한 (PARAMEDIC, HOSPITAL, ADMIN 등)

    public StompPrincipal(String name, String role) {
        this.name = name;
        this.role = role;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }

    @Override
    public String toString() {
        return "StompPrincipal{name='" + name + "', role='" + role + "'}";
    }
}
