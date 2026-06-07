package com.emergencymatching.auth.service;

import com.emergencymatching.auth.domain.Member;
import com.emergencymatching.auth.dto.AuthDto;
import com.emergencymatching.auth.exception.AuthException;
import com.emergencymatching.auth.repository.MemberRepository;
import com.emergencymatching.auth.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(MemberRepository memberRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public void signup(AuthDto.SignupRequest request) {
        if (memberRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }

        Member member = new Member(
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                request.getRole()
        );
        
        memberRepository.save(member);
    }

    public AuthDto.TokenResponse login(AuthDto.LoginRequest request) {
        Member member = memberRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AuthException("가입되지 않은 아이디입니다.", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new AuthException("잘못된 비밀번호입니다.", HttpStatus.UNAUTHORIZED);
        }

        String token = jwtTokenProvider.createToken(member.getId(), member.getUsername(), member.getRole().name());
        return new AuthDto.TokenResponse(token);
    }
}
