package com.emergencymatching.apigateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private JwtAuthenticationFilter jwtAuthenticationFilter;
    private GatewayFilter filter;
    private GatewayFilterChain filterChain;
    
    private final String testSecretKey = "defaultSecretKeyForEmergencyMatchingSystemVeryLongKey1234567890!@#";
    private SecretKey key;

    @BeforeEach
    void setUp() {
        // 필터 초기화
        jwtAuthenticationFilter = new JwtAuthenticationFilter(testSecretKey);
        filter = jwtAuthenticationFilter.apply(new JwtAuthenticationFilter.Config());
        
        // Mock 필터 체인 (통과 시 호출됨)
        filterChain = mock(GatewayFilterChain.class);
        when(filterChain.filter(any())).thenReturn(Mono.empty());
        
        // 키 초기화
        key = Keys.hmacShaKeyFor(testSecretKey.getBytes(StandardCharsets.UTF_8));
    }

    private String generateValidToken() {
        Date now = new Date();
        Date validity = new Date(now.getTime() + 3600000); // 1시간 후 만료

        return Jwts.builder()
                .subject("testUser")
                .claim("id", 1L)
                .claim("role", "HOSPITAL")
                .issuedAt(now)
                .expiration(validity)
                .signWith(key)
                .compact();
    }

    @Test
    @DisplayName("성공: 올바른 토큰이 들어오면 필터를 통과하고 헤더가 추가된다")
    void filter_success_withValidToken() {
        // given
        String validToken = generateValidToken();
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/some-endpoint")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // when & then
        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        org.mockito.ArgumentCaptor<org.springframework.web.server.ServerWebExchange> exchangeCaptor =
                org.mockito.ArgumentCaptor.forClass(org.springframework.web.server.ServerWebExchange.class);
        org.mockito.Mockito.verify(filterChain).filter(exchangeCaptor.capture());
        org.springframework.web.server.ServerWebExchange capturedExchange = exchangeCaptor.getValue();
        assertThat(capturedExchange.getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("1");
        assertThat(capturedExchange.getRequest().getHeaders().getFirst("X-User-Name")).isEqualTo("testUser");
        assertThat(capturedExchange.getRequest().getHeaders().getFirst("X-User-Role")).isEqualTo("HOSPITAL");
    }

    @Test
    @DisplayName("성공: Query Parameter로 토큰이 들어오면 필터를 통과하고 헤더가 추가된다 (웹소켓 용)")
    void filter_success_withTokenQueryParam() {
        // given
        String validToken = generateValidToken();
        MockServerHttpRequest request = MockServerHttpRequest.get("/ws")
                .queryParam("token", validToken)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // when & then
        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();

        org.mockito.ArgumentCaptor<org.springframework.web.server.ServerWebExchange> exchangeCaptor =
                org.mockito.ArgumentCaptor.forClass(org.springframework.web.server.ServerWebExchange.class);
        org.mockito.Mockito.verify(filterChain).filter(exchangeCaptor.capture());
        org.springframework.web.server.ServerWebExchange capturedExchange = exchangeCaptor.getValue();
        assertThat(capturedExchange.getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("1");
        assertThat(capturedExchange.getRequest().getHeaders().getFirst("X-User-Name")).isEqualTo("testUser");
        assertThat(capturedExchange.getRequest().getHeaders().getFirst("X-User-Role")).isEqualTo("HOSPITAL");
    }

    @Test
    @DisplayName("실패: Authorization 헤더가 아예 없는 경우 401 UNAUTHORIZED 반환")
    void filter_fail_withoutHeader() {
        // given
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/some-endpoint")
                .build(); // 헤더 없음
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // when
        Mono<Void> result = filter.filter(exchange, filterChain);

        // then
        StepVerifier.create(result).verifyComplete(); // onError가 세팅되어 complete됨
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("실패: Bearer 형식이 아닌 경우 401 UNAUTHORIZED 반환")
    void filter_fail_invalidHeaderFormat() {
        // given
        String validToken = generateValidToken();
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/some-endpoint")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + validToken) // Bearer가 아님
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // when
        Mono<Void> result = filter.filter(exchange, filterChain);

        // then
        StepVerifier.create(result).verifyComplete();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("실패: 유효하지 않은 토큰(위조)인 경우 401 UNAUTHORIZED 반환")
    void filter_fail_invalidToken() {
        // given
        String fakeToken = "thisIsFakeToken.ButVeryLongString.ForTestingPurpose";
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/some-endpoint")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + fakeToken)
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // when
        Mono<Void> result = filter.filter(exchange, filterChain);

        // then
        StepVerifier.create(result).verifyComplete();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
