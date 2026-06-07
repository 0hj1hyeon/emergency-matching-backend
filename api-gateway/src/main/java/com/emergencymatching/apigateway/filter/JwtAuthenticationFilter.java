package com.emergencymatching.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private final SecretKey key;

    public JwtAuthenticationFilter(@Value("${jwt.secret}") String secretKey) {
        super(Config.class);
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public static class Config {
        // 설정값 추가
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // 1. Authorization 헤더 또는 쿼리 파라미터에서 토큰 추출
            String token = null;
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.replace("Bearer ", "");
            } else {
                token = request.getQueryParams().getFirst("token");
            }

            if (token == null) {
                return onError(exchange.getResponse(), "No token found", HttpStatus.UNAUTHORIZED);
            }

            // 2. 토큰 검증
            try {
                Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                // 3. 토큰에서 정보 추출하여 헤더에 추가
                String username = claims.getSubject();
                Long id = claims.get("id", Long.class);
                String role = claims.get("role", String.class);

                ServerHttpRequest modifiedRequest = request.mutate()
                        .header("X-User-Id", id != null ? id.toString() : "")
                        .header("X-User-Name", username)
                        .header("X-User-Role", role)
                        .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());

            } catch (Exception e) {
                // 토큰 만료 또는 위조 시 에러 처리
                return onError(exchange.getResponse(), "JWT validation failed", HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private Mono<Void> onError(ServerHttpResponse response, String err, HttpStatus httpStatus) {
        response.setStatusCode(httpStatus);
        return response.setComplete();
    }
}
