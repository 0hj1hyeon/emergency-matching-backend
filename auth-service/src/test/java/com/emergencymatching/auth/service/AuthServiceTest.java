package com.emergencymatching.auth.service;

import com.emergencymatching.auth.domain.Member;
import com.emergencymatching.auth.domain.MemberRole;
import com.emergencymatching.auth.dto.AuthDto;
import com.emergencymatching.auth.exception.AuthException;
import com.emergencymatching.auth.repository.MemberRepository;
import com.emergencymatching.auth.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("회원가입 성공")
    void signup_success() {
        // given
        AuthDto.SignupRequest request = new AuthDto.SignupRequest("user1", "pass123", MemberRole.HOSPITAL);
        given(memberRepository.existsByUsername(request.getUsername())).willReturn(false);
        given(passwordEncoder.encode(request.getPassword())).willReturn("encodedPass");

        // when
        authService.signup(request);

        // then
        verify(memberRepository).save(any(Member.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 중복 아이디")
    void signup_fail_duplicate() {
        // given
        AuthDto.SignupRequest request = new AuthDto.SignupRequest("user1", "pass123", MemberRole.HOSPITAL);
        given(memberRepository.existsByUsername(request.getUsername())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 존재하는 아이디입니다.");
    }

    @Test
    @DisplayName("로그인 성공")
    void login_success() {
        // given
        AuthDto.LoginRequest request = new AuthDto.LoginRequest("user1", "pass123");
        Member member = new Member("user1", "encodedPass", MemberRole.HOSPITAL);
        org.springframework.test.util.ReflectionTestUtils.setField(member, "id", 1L);

        given(memberRepository.findByUsername(request.getUsername())).willReturn(Optional.of(member));
        given(passwordEncoder.matches(request.getPassword(), member.getPassword())).willReturn(true);
        given(jwtTokenProvider.createToken(1L, member.getUsername(), member.getRole().name())).willReturn("test-token");

        // when
        AuthDto.TokenResponse response = authService.login(request);

        // then
        assertThat(response.getAccessToken()).isEqualTo("test-token");
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호")
    void login_fail_password() {
        // given
        AuthDto.LoginRequest request = new AuthDto.LoginRequest("user1", "wrongPass");
        Member member = new Member("user1", "encodedPass", MemberRole.HOSPITAL);

        given(memberRepository.findByUsername(request.getUsername())).willReturn(Optional.of(member));
        given(passwordEncoder.matches(request.getPassword(), member.getPassword())).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthException.class)
                .hasMessage("잘못된 비밀번호입니다.");
    }
}
