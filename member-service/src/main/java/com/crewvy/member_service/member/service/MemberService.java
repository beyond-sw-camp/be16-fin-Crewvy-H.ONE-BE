package com.crewvy.member_service.member.service;

import com.crewvy.member_service.common.auth.JwtTokenProvider;
import com.crewvy.member_service.common.constant.YnColumn;
import com.crewvy.member_service.member.dto.request.CreateAdminReqDto;
import com.crewvy.member_service.member.dto.request.CreateMemberReqDto;
import com.crewvy.member_service.member.dto.request.LoginReqDto;
import com.crewvy.member_service.member.dto.response.LoginResDto;
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
    public UUID createAdmin(CreateAdminReqDto createAdminReqDto){
        if (memberRepository.findByEmail(createAdminReqDto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("사용할 수 없는 이메일입니다.");
        }

        if (!createAdminReqDto.getPassword().equals(createAdminReqDto.getCheckPw())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        String encodePassword = passwordEncoder.encode(createAdminReqDto.getPassword());

        // 테스트용 회사
        Company testCompany = Company.builder().companyName("테스트회사").build();
        companyRepository.save(testCompany);

        Member savedMember = memberRepository.save(createAdminReqDto.toEntity(encodePassword, testCompany));
        return savedMember.getMemberId();
    }

    // 사용자 계정 생성
    public UUID createMember(CreateMemberReqDto createMemberRequestDto) {
        if (memberRepository.findByEmail(createMemberRequestDto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("사용할 수 없는 이메일입니다.");
        }

        String encodePassword = passwordEncoder.encode(createMemberRequestDto.getPassword());

        // 테스트용 회사
        Company testCompany = Company.builder().companyName("테스트회사").build();
        companyRepository.save(testCompany);

        Member savedMember = memberRepository.save(createMemberRequestDto.toEntity(encodePassword, testCompany));
        return savedMember.getMemberId();
    }

    // 로그인
    public LoginResDto doLogin(LoginReqDto loginReqDto) {
        Member member = memberRepository.findByEmail(loginReqDto.getEmail()).orElseThrow(()
                -> new IllegalArgumentException("email 또는 비밀번호가 일치하지 않습니다."));

        if (!passwordEncoder.matches(loginReqDto.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException("email 또는 비밀번호가 일치하지 않습니다.");
        }

        if (member.getYnDel().equals(YnColumn.isTrue)) {
            throw new IllegalArgumentException("탈퇴한 회원입니다.");
        }

        String accessToken = jwtTokenProvider.createAtToken(member);
//        String refreshToken = jwtTokenProvider.createRtToken(member);
        return LoginResDto.builder()
                .accessToken(accessToken)
//                .refreshToken(refreshToken)
                .build();
    }

//    // AT 재발급
//    public LoginResDto generateNewAt(GenerateNewAtDto generateNewAtDto) {
//        Member member = jwtTokenProvider.validateRt(generateNewAtDto.getRefreshToken());
//
//        String accessToken = jwtTokenProvider.createAtToken(member);
//        return LoginResDto.builder()
//                .accessToken(accessToken)
//                .build();
//    }
}
