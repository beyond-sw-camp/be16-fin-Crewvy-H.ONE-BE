package com.crewvy.member_service.common.auth;

import com.crewvy.member_service.member.entity.Member;
import com.crewvy.member_service.member.repository.MemberRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {
    private final MemberRepository memberRepository;
//    private final RedisTemplate<String, String> redisTemplate;

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
    public JwtTokenProvider(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @PostConstruct
    public void init() {
        byte[] atByte = java.util.Base64.getDecoder().decode(secretKeyAt);
        this.atKey = Keys.hmacShaKeyFor(atByte);
        byte[] rtByte = java.util.Base64.getDecoder().decode(secretKeyRt);
        this.rtKey = Keys.hmacShaKeyFor(rtByte);
    }

    public String createAtToken(Member member) {
        String email = member.getEmail();

        Claims claims = Jwts.claims().setSubject(email);

        Date now = new Date();
        String accessToken = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationAt*60*1000L))
                .signWith(atKey)
                .compact();

        return accessToken;
    }

//    public String createRtToken(Member member) {
//        String email = member.getEmail();
//
//        Claims claims = Jwts.claims().setSubject(email);
//
//        Date now = new Date();
//        String refreshToken = Jwts.builder()
//                .setClaims(claims)
//                .setIssuedAt(now)
//                .setExpiration(new Date(now.getTime() + expirationRt*60*1000L))
//                .signWith(rtKey)
//                .compact();
//
//        redisTemplate.opsForValue().set(member.getEmail(), refreshToken, 200, TimeUnit.DAYS);
//
//        return refreshToken;
//    }
//
//    public Member validateRt(String refreshToken) {
//        Claims claims = Jwts.parserBuilder()
//                .setSigningKey(rtKey)
//                .build()
//                .parseClaimsJws(refreshToken)
//                .getBody();
//
//        String email = claims.getSubject();
//        Member member = memberRepository.findByEmail(email).orElseThrow(() -> new EntityNotFoundException("없는 사용자"));
//
//        String redisRt = redisTemplate.opsForValue().get(member.getEmail());
//        if(!redisRt.equals(refreshToken)) {
//            throw new IllegalArgumentException("잘못된 토큰입니다.");
//        }
//
//        return member;
//    }

}
