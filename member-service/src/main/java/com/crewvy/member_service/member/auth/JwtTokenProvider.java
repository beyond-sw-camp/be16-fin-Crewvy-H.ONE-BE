package com.crewvy.member_service.member.auth;

import com.crewvy.member_service.member.entity.Member;
import com.crewvy.member_service.member.entity.MemberPosition;
import com.crewvy.member_service.member.repository.MemberRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class JwtTokenProvider {
    private final MemberRepository memberRepository;
    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.expirationAt}")
    private int expirationAt;

    @Value("${jwt.secretKeyAt}")
    private String secretKeyAt;

    @Value("${jwt.expirationRt}")
    private int expirationRt;

    @Value("${jwt.secretKeyRt}")
    private String secretKeyRt;

    private Key atKey;
    private Key rtKey;

    @Autowired
    public JwtTokenProvider(MemberRepository memberRepository, @Qualifier("rtInventory") StringRedisTemplate rtRedisTemplate) {
        this.memberRepository = memberRepository;
        this.redisTemplate = rtRedisTemplate;
    }

    @PostConstruct
    public void init() {
        byte[] atByte = java.util.Base64.getDecoder().decode(secretKeyAt);
        this.atKey = Keys.hmacShaKeyFor(atByte);
        byte[] rtByte = java.util.Base64.getDecoder().decode(secretKeyRt);
        this.rtKey = Keys.hmacShaKeyFor(rtByte);
    }

    public String createAtToken(Member member, MemberPosition memberPosition) {
        String memberId = member.getId().toString();
        String memberPositionId = memberPosition.getId().toString();
        String organizationId = memberPosition.getOrganization().getId().toString();
        String companyId = member.getCompany().getId().toString();
        String name = member.getName();

        Claims claims = Jwts.claims().setSubject(memberId);
        claims.put("memberPositionId", memberPositionId);
        claims.put("organizationId", organizationId);
        claims.put("companyId", companyId);
        claims.put("name", name);

        Date now = new Date();
        String accessToken = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationAt*60*1000L))
                .signWith(atKey)
                .compact();

        log.info("Created JWT token for member '{}'", name);

        return accessToken;
    }

    public String createRtToken(Member member) {
        String email = member.getEmail();

        Claims claims = Jwts.claims().setSubject(member.getId().toString());
        claims.put("email", email);

        Date now = new Date();
        String refreshToken = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationRt*60*1000L))
                .signWith(rtKey)
                .compact();

        redisTemplate.opsForValue().set(member.getEmail(), refreshToken, 200, TimeUnit.DAYS);

        return refreshToken;
    }

    public Member validateRt(String refreshToken, String atMemberId) {
        Claims rtClaims = getClaims(refreshToken, rtKey);

        String rtMemberId = rtClaims.getSubject();

        if (!rtMemberId.equals(atMemberId)) {
            throw new IllegalArgumentException("사용자 정보가 일치하지 않습니다.");
        }

        try {
            Jwts.parserBuilder().setSigningKey(rtKey).build().parseClaimsJws(refreshToken);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw new IllegalArgumentException("토큰이 만료되었습니다.");
        }

        Member member = memberRepository.findById(UUID.fromString(rtMemberId))
                .orElseThrow(() -> new EntityNotFoundException("없는 사용자입니다."));

        String redisRt = redisTemplate.opsForValue().get(member.getEmail());
        if (redisRt == null || !redisRt.equals(refreshToken)) {
            throw new IllegalArgumentException("잘못된 토큰입니다.");
        }

        return member;
    }

    private Claims getClaims(String token, Key key) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return e.getClaims();
        }
    }
}
