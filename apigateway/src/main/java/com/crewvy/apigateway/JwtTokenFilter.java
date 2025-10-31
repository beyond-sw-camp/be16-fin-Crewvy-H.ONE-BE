package com.crewvy.apigateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.apache.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.security.Key;
import java.util.List;

@Component
public class JwtTokenFilter implements GlobalFilter {
    @Value("${jwt.secretKeyAt}")
    private String secretKeyAt;
    private Key atKey;

    @PostConstruct
    public void init() {
        byte[] atByte = java.util.Base64.getDecoder().decode(secretKeyAt);
        this.atKey = Keys.hmacShaKeyFor(atByte);
    }

    private static final List<String> ALLOWED_PATH = List.of(
            "/member/login",
            "/member/create-admin",
            "/member/check-email",
            "/member/reset-password",
            "/member/check-business-number",
            "/actuator/health",
            "/openvidu-webhooks",
            "/livekit/webhook"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String bearerToken = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        System.out.println("bearerToken: " + bearerToken);

        String urlPath = exchange.getRequest().getURI().getRawPath();
        System.out.println("UrlPath: " + urlPath);

        if (ALLOWED_PATH.contains((urlPath))) {
            return chain.filter(exchange);
        }

        try {
            if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
                throw new IllegalArgumentException("토큰이 없거나 형식이 잘못 되었습니다.");
            }

            String token = bearerToken.substring(7);

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(atKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String memberId = claims.getSubject();
            String memberPositionId = claims.get("memberPositionId", String.class);
            String organizationId = claims.get("organizationId", String.class);
            String companyId = claims.get("companyId", String.class);
            String name = claims.get("name", String.class);

            ServerWebExchange serverWebExchange = exchange.mutate()
                    .request(r -> r
                            .header("X-User-UUID", memberId)
                            .header("X-User-MemberPositionId", memberPositionId)
                            .header("X-User-OrganizationId", organizationId)
                            .header("X-User-CompanyId", companyId))
                    .build();

            return chain.filter(serverWebExchange);
        } catch (Exception e) {
            e.printStackTrace();
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }
}
