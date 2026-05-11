package com.emergencymatching.auth.controller;

import com.emergencymatching.auth.dto.AuthDto;
import com.emergencymatching.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<String> signup(@Valid @RequestBody AuthDto.SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.ok("회원가입이 완료되었습니다.");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDto.TokenResponse> login(@Valid @RequestBody AuthDto.LoginRequest request) {
        AuthDto.TokenResponse tokenResponse = authService.login(request);
        return ResponseEntity.ok(tokenResponse);
    }
}
