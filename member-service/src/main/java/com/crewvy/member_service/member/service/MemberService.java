package com.crewvy.member_service.member.service;

import com.crewvy.member_service.common.auth.JwtTokenProvider;
import com.crewvy.member_service.common.constant.Bool;
import com.crewvy.member_service.member.dto.request.CreateAdminReq;
import com.crewvy.member_service.member.dto.request.CreateMemberReq;
import com.crewvy.member_service.member.dto.request.LoginReq;
import com.crewvy.member_service.member.dto.response.LoginRes;
import com.crewvy.member_service.member.entity.Company;
import com.crewvy.member_service.member.entity.Member;
import com.crewvy.member_service.member.repository.CompanyRepository;
import com.crewvy.member_service.member.repository.MemberRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public MemberService(MemberRepository memberRepository, CompanyRepository companyRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.memberRepository = memberRepository;
        this.companyRepository = companyRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    // 관리자 계정 생성
    public UUID createAdmin(CreateAdminReq createAdminReq) {
        if (memberRepository.findByEmail(createAdminReq.getEmail()).isPresent()) {
            throw new IllegalArgumentException("사용할 수 없는 이메일입니다.");
        }

        if (!createAdminReq.getPassword().equals(createAdminReq.getCheckPw())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        String encodePassword = passwordEncoder.encode(createAdminReq.getPassword());

        Company company = Company.builder().companyName(createAdminReq.getCompanyName()).build();
        companyRepository.save(company);

        Member savedMember = memberRepository.save(createAdminReq.toEntity(encodePassword, company));
        return savedMember.getId();
    }

    // 사용자 계정 생성
    public UUID createMember(String email, CreateMemberReq createMemberReq) {
        if (memberRepository.findByEmail(createMemberReq.getEmail()).isPresent()) {
            throw new IllegalArgumentException("사용할 수 없는 이메일입니다.");
        }

        String encodePassword = passwordEncoder.encode(createMemberReq.getPassword());
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        System.out.println(member);
        Company company = companyRepository.findByCompanyName(member.getCompany().getCompanyName())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회사명입니다."));

        Member savedMember = memberRepository.save(createMemberReq.toEntity(encodePassword, company));
        return savedMember.getId();
    }

    // 로그인
    public LoginRes doLogin(LoginReq loginReq) {
        Member member = memberRepository.findByEmail(loginReq.getEmail()).orElseThrow(()
                -> new IllegalArgumentException("email 또는 비밀번호가 일치하지 않습니다."));

        if (!passwordEncoder.matches(loginReq.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException("email 또는 비밀번호가 일치하지 않습니다.");
        }

        if (member.getYnDel().equals(Bool.TRUE)) {
            throw new IllegalArgumentException("탈퇴한 회원입니다.");
        }

        String accessToken = jwtTokenProvider.createAtToken(member);
//        String refreshToken = jwtTokenProvider.createRtToken(member);
        return LoginRes.builder()
                .accessToken(accessToken)
//                .refreshToken(refreshToken)
                .build();
    }

//    // AT 재발급
//    public LoginResDto generateNewAt(GenerateNewAtReq generateNewAtReq) {
//        Member member = jwtTokenProvider.validateRt(generateNewAtReq.getRefreshToken());
//
//        String accessToken = jwtTokenProvider.createAtToken(member);
//        return LoginRes.builder()
//                .accessToken(accessToken)
//                .build();
//    }
}
