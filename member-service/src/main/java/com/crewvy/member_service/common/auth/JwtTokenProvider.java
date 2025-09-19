package com.crewvy.member_service.common.auth;

import com.crewvy.member_service.member.repository.MemberRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {
    private final MemberRepository memberRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${jwt.expirationAt}")
    private int expirationAt;
    @Value("${jwt.secretKeyAt}")
    private String secretKeyAt;
    @Value("${jwt.expirationRt}")
    private int expirationRt;
    @Value("${jwt.secretKeyRt}")
    private String secretKeyRt;

    private Key secretAtKey;
    private Key secretRtKey;

    public JwtTokenProvider(MemberRepository memberRepository, @Qualifier("rtInventory") RedisTemplate<String, String> redisTemplate) {
        this.memberRepository = memberRepository;
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init() {
        // 기존보다 키 길이에 대한 유효성 검사 등 부가적인 안전장치 제공
        byte[] atByte = java.util.Base64.getDecoder().decode(secretKeyAt);
        this.secretAtKey = Keys.hmacShaKeyFor(atByte);
        byte[] rtByte = java.util.Base64.getDecoder().decode(secretKeyRt);
        this.secretRtKey = Keys.hmacShaKeyFor(rtByte);
    }

    public String createAtToken(Member member) {
        long expiration = member.isAdditionalInfoRequired() ? 10L : expirationAt;

        String email = member.getEmail();
        String roleCode = member.getRoleCode().toString();
        String socialType = member.getSocialType().getCodeValue();

        Claims claims = Jwts.claims().setSubject(email);
        claims.put("roleCode", roleCode);
        claims.put("socialType", socialType);
        Date now = new Date();
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expiration * 60 * 1000L))
                .signWith(this.secretAtKey)
                .compact();
    }

    public String createRtToken(Member member) {
        String email = member.getEmail();
        String roleCode = member.getRoleCode().toString();
        String socialType = member.getSocialType().getCodeValue();

        Claims claims = Jwts.claims().setSubject(email);
        claims.put("roleCode", roleCode);
        claims.put("socialType", socialType);
        Date now = new Date();
        String refreshToken = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationRt * 60 * 1000L))
                .signWith(this.secretRtKey)
                .compact();

        String redisKey = email + ":" + socialType;
        redisTemplate.opsForValue().set(redisKey, refreshToken);
        return refreshToken;
    }

    public Member validateRt(String refreshToken) {
        Claims claims;
        try {
            claims = Jwts.parserBuilder()
                    .setSigningKey(secretRtKey)
                    .build()
                    .parseClaimsJws(refreshToken)
                    .getBody();
        } catch(SignatureException e){
            throw new IllegalArgumentException("유효하지 않은 token 입니다.");
        }

        String email = claims.getSubject();
        Member member = memberRepository.findByEmail(emailThrow(() -> new EntityNotFoundException("존재하지 않는 회원입니다."));
        String redisRt = redisTemplate.opsForValue().get();

        if (redisRt == null || !redisRt.equals(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 token 입니다.");
        }
        return member;
    }
}
