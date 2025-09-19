package com.crewvy.member_service.member.service;

import com.chillex.gooseBumps.common.auth.JwtTokenProvider;
import com.chillex.gooseBumps.common.component.ImageDownloader;
import com.chillex.gooseBumps.common.component.ImageDownloader.ImageDownloadResult;
import com.chillex.gooseBumps.common.constant.code.member.LoginStatus;
import com.chillex.gooseBumps.common.constant.code.member.SocialType;
import com.chillex.gooseBumps.common.constant.code.music.PlayListType;
import com.chillex.gooseBumps.common.constant.entity.YnColumn;
import com.chillex.gooseBumps.common.file.AwsS3Uploader;
import com.chillex.gooseBumps.domain.member.dto.request.*;
import com.chillex.gooseBumps.domain.member.dto.response.MemberListDto;
import com.chillex.gooseBumps.domain.member.dto.response.MemberLoginResDto;
import com.chillex.gooseBumps.domain.member.dto.response.MyPageDto;
import com.chillex.gooseBumps.domain.member.entity.Member;
import com.chillex.gooseBumps.domain.member.repository.MemberRepository;
import com.chillex.gooseBumps.domain.playlist.entity.PlayList;
import com.neovisionaries.i18n.CountryCode;
import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@Slf4j
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AwsS3Uploader awsS3Uploader;
    private final ImageDownloader imageDownloader;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${oauth.google.client-id}")
    private String googleClientId;

    @Value("${oauth.google.client-secret}")
    private String googleClientSecret;

    @Value("${oauth.google.redirect-uri}")
    private String googleRedirectUri;

    @Value("${oauth.kakao.client-id}")
    private String kakaoClientId;

    @Value("${oauth.kakao.redirect-uri}")
    private String kakaoRedirectUri;

    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider,
                         AwsS3Uploader awsS3Uploader, ImageDownloader imageDownloader) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.awsS3Uploader = awsS3Uploader;
        this.imageDownloader = imageDownloader;
    }

    public Long saveMemberAndPlayLists(MemberCreateDto memberCreateDto) {
        if (memberRepository.findByEmailAndSocialType(memberCreateDto.getEmail(),
                SocialType.fromCode(memberCreateDto.getSocialType())).isPresent()) {
            throw new IllegalArgumentException("사용할 수 없는 이메일입니다.");
        }

        if (!memberCreateDto.getPassword().equals(memberCreateDto.getCheckPw())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        if (CountryCode.getByCode(Integer.parseInt(memberCreateDto.getNationalityCode())) == null) {
            throw new IllegalArgumentException("잘못된 국가코드입니다.");
        }

        String encodePassword = passwordEncoder.encode(memberCreateDto.getPassword());
        Member member = memberCreateDto.toEntity(encodePassword);
        memberRepository.save(member);

        PlayList defaultPlayList = PlayList.builder()
                .playListTypeCode(PlayListType.STAND_QUEUE)
                .playListName(PlayListType.STAND_QUEUE.getCodeName())
                .member(member)
                .build();
        member.getPlayListList().add(defaultPlayList);

        PlayList likedPlayList = PlayList.builder()
                .playListTypeCode(PlayListType.LIKE)
                .playListName(PlayListType.LIKE.getCodeName())
                .member(member)
                .build();
        member.getPlayListList().add(likedPlayList);

        if (memberCreateDto.getProfileImage() != null && !memberCreateDto.getProfileImage().isEmpty()) {
            member.updateImageUrl(awsS3Uploader.uploadFile("member", member.getMemberSeq(), memberCreateDto.getProfileImage()));
        }
        return member.getMemberSeq();
    }

    @Transactional(readOnly = true)
    public MyPageDto myPage() {
        return MyPageDto.fromEntity(findMember());
    }

    public Long updateMember(MemberUpdateDto memberUpdateDto) {
        Member member = findMember();
        String encodePw = member.getPassword();
        String newPassword = memberUpdateDto.getPassword();

        if (newPassword != null && !newPassword.isBlank()) {
            if (!newPassword.equals(memberUpdateDto.getCheckPw())) {
                throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
            }

            if (newPassword.length() < 8){
                throw new IllegalArgumentException("비밀번호가 너무 짧습니다.");
            }

            if (passwordEncoder.matches(newPassword, encodePw)) {
                throw new IllegalArgumentException("기존 비밀번호와 동일한 비밀번호로 변경할 수 없습니다.");
            }

            encodePw = passwordEncoder.encode(newPassword);
        }

        if (CountryCode.getByCode(Integer.parseInt(memberUpdateDto.getNationalityCode())) == null) {
            throw new IllegalArgumentException("잘못된 국가코드입니다.");
        }

        member.updateMember(memberUpdateDto, encodePw);

        if (memberUpdateDto.getProfileImage() != null && !memberUpdateDto.getProfileImage().isEmpty()) {
            if (member.getProfileImageUrl() != null && !member.getProfileImageUrl().isEmpty()) {
                awsS3Uploader.deleteFile(member.getProfileImageUrl());
            }
            member.updateImageUrl(awsS3Uploader.uploadFile("member", member.getMemberSeq(), memberUpdateDto.getProfileImage()));
        } else if (memberUpdateDto.getDelProfile() && member.getProfileImageUrl() != null && !member.getProfileImageUrl().isEmpty()) {
            awsS3Uploader.deleteFile(member.getProfileImageUrl());
            member.updateImageUrl(null);
        }

        return member.getMemberSeq();
    }

    @Transactional(readOnly = true)
    public Page<MemberListDto> findAll(Pageable pageable, MemberSearchDto memberSearchDto) {
        Specification<Member> specification = new Specification<Member>() {
            @Override
            public Predicate toPredicate(Root<Member> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                List<Predicate> predicateList = new ArrayList<>();

                if (memberSearchDto.getEmail() != null) {
                    predicateList.add(criteriaBuilder.equal(root.get("email"), memberSearchDto.getEmail()));
                }
                if (memberSearchDto.getName() != null) {
                    predicateList.add(criteriaBuilder.like(root.get("name"), "%" + memberSearchDto.getName() + "%"));
                }

                Predicate[] predicateArr = new Predicate[predicateList.size()];
                for (int i = 0; i < predicateList.size(); i++) {
                    predicateArr[i] = predicateList.get(i);
                }

                Predicate predicate = criteriaBuilder.and(predicateArr);
                return predicate;
            }
        };
        return memberRepository.findAll(specification, pageable).map(m -> MemberListDto.fromEntity(m));
    }

    @Transactional(readOnly = true)
    public MyPageDto findByMemberSeq(Long memberSeq) {
        return MyPageDto.fromEntity(memberRepository.findById(memberSeq).orElseThrow(()
                -> new EntityNotFoundException("존재하지 않는 회원입니다.")));
    }

    public void deleteMember() {
        findMember().deleteMember();
    }

    public MemberLoginResDto login(MemberLoginReqDto memberLoginDto) {
        Member member = memberRepository.findByEmailAndSocialType(memberLoginDto.getEmail(), SocialType.GOOSEBUMPS).orElseThrow(()
                -> new IllegalArgumentException("email 또는 비밀번호가 일치하지 않습니다."));
        if (member.getYnDel().equals(YnColumn.isTrue)) {
            throw new IllegalArgumentException("탈퇴한 회원입니다.");
        }

        if (!passwordEncoder.matches(memberLoginDto.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException("email 또는 비밀번호가 일치하지 않습니다.");
        }

        if (member.isAdditionalInfoRequired()) {
            String accessToken = jwtTokenProvider.createAtToken(member);
            return MemberLoginResDto.builder()
                    .status(LoginStatus.INCOMPLETE)
                    .accessToken(accessToken)
                    .build();
        }

        String accessToken = jwtTokenProvider.createAtToken(member);
        String refreshToken = jwtTokenProvider.createRtToken(member);
        return MemberLoginResDto.builder()
                .status(LoginStatus.COMPLETE)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public void resetPw(FindPwDto findPwDto) {
        Member member = memberRepository.findByEmailAndSocialType(findPwDto.getEmail(), SocialType.GOOSEBUMPS).orElseThrow(()
                -> new EntityNotFoundException("회원 정보가 일치하지 않습니다."));
        if (member.getTelNo().equals(findPwDto.getTelNo())) {
            if (findPwDto.getNewPw().equals(findPwDto.getCheckNewPw())) {
                String encodeNewPw = passwordEncoder.encode(findPwDto.getNewPw());
                member.resetPw(encodeNewPw);
            } else {
                throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
            }
        } else {
            throw new IllegalArgumentException("회원 정보가 일치하지 않습니다.");
        }
    }

    public MemberLoginResDto generateNewAt(GenerateNewAtDto generateNewAtDto) {
        Member member = jwtTokenProvider.validateRt(generateNewAtDto.getRefreshToken());

        String accessToken = jwtTokenProvider.createAtToken(member);
        return MemberLoginResDto.builder()
                .accessToken(accessToken)
                .build();
    }

    public MemberLoginResDto googleLogin(ProvideCodeDto provideCodeDto) {
        RestClient restClient = RestClient.create();
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", provideCodeDto.getCode());
        params.add("client_id", googleClientId);
        params.add("client_secret", googleClientSecret);
        params.add("redirect_uri", googleRedirectUri);
        params.add("grant_type", "authorization_code");

        ResponseEntity<OauthTokenDto> googleRes = restClient.post()
                .uri("https://oauth2.googleapis.com/token")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body(params)
                .retrieve()
                .toEntity(OauthTokenDto.class);

        RestClient profileRest = RestClient.create();
        ResponseEntity<GoogleProfileDto> profile = profileRest.get()
                .uri("https://openidconnect.googleapis.com/v1/userinfo")
                .header("Authorization", "Bearer " + googleRes.getBody().getAccess_token())
                .retrieve()
                .toEntity(GoogleProfileDto.class);

        Member member = memberRepository.findBySocialId(profile.getBody().getSub()).orElse(null);

        if (member == null) {
            Member newMember = Member.builder()
                    .email(profile.getBody().getEmail())
                    .name(profile.getBody().getName())
                    .socialType(SocialType.GOOGLE)
                    .socialId(profile.getBody().getSub())
                    .profileImageUrl(profile.getBody().getPicture())
                    .build();
            memberRepository.save(newMember);

            PlayList defaultPlayList = PlayList.builder()
                    .playListTypeCode(PlayListType.STAND_QUEUE)
                    .playListName(PlayListType.STAND_QUEUE.getCodeName())
                    .member(newMember)
                    .build();
            newMember.getPlayListList().add(defaultPlayList);

            PlayList likedPlayList = PlayList.builder()
                    .playListTypeCode(PlayListType.LIKE)
                    .playListName(PlayListType.LIKE.getCodeName())
                    .member(newMember)
                    .build();
            newMember.getPlayListList().add(likedPlayList);

            ImageDownloadResult downloadResult = imageDownloader.downloadImage(profile.getBody().getPicture());
            newMember.updateImageUrl(awsS3Uploader.uploadFile("member", newMember.getMemberSeq(),
                    downloadResult.getInputStream(), downloadResult.getContentType(), downloadResult.getFilename()));

            member = newMember;
        }

        if (member.getYnDel().equals("Y")){
            throw new IllegalArgumentException("이미 탈퇴한 회원입니다.");
        }

        if (member.isAdditionalInfoRequired()) {
            String accessToken = jwtTokenProvider.createAtToken(member);
            return MemberLoginResDto.builder()
                    .status(LoginStatus.INCOMPLETE)
                    .accessToken(accessToken)
                    .build();
        }

        String accessToken = jwtTokenProvider.createAtToken(member);
        String refreshToken = jwtTokenProvider.createRtToken(member);
        return MemberLoginResDto.builder()
                .status(LoginStatus.COMPLETE)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public MemberLoginResDto kakaoLogin(ProvideCodeDto provideCodeDto) {
        RestClient restClient = RestClient.create();
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", provideCodeDto.getCode());
        params.add("client_id", kakaoClientId);
        params.add("redirect_uri", kakaoRedirectUri);
        params.add("grant_type", "authorization_code");

        ResponseEntity<OauthTokenDto> kakaoRes = restClient.post()
                .uri("https://kauth.kakao.com/oauth/token")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body(params)
                .retrieve()
                .toEntity(OauthTokenDto.class);

        RestClient profileRest = RestClient.create();
        ResponseEntity<KakaoProfileDto> profile = profileRest.get()
                .uri("https://kapi.kakao.com/v2/user/me")
                .header("Authorization", "Bearer " + kakaoRes.getBody().getAccess_token())
                .retrieve()
                .toEntity(KakaoProfileDto.class);

        Member member = memberRepository.findBySocialId(profile.getBody().getId()).orElse(null);

        if (member == null) {
            Member newMember = Member.builder()
                    .email(profile.getBody().getKakao_account().getEmail())
                    .name(profile.getBody().getKakao_account().getProfile().getNickname())
                    .socialType(SocialType.KAKAO)
                    .socialId(profile.getBody().getId())
                    .build();
            memberRepository.save(newMember);

            PlayList defaultPlayList = PlayList.builder()
                    .playListTypeCode(PlayListType.STAND_QUEUE)
                    .playListName(PlayListType.STAND_QUEUE.getCodeName())
                    .member(newMember)
                    .build();
            newMember.getPlayListList().add(defaultPlayList);

            PlayList likedPlayList = PlayList.builder()
                    .playListTypeCode(PlayListType.LIKE)
                    .playListName(PlayListType.LIKE.getCodeName())
                    .member(newMember)
                    .build();
            newMember.getPlayListList().add(likedPlayList);

            ImageDownloadResult downloadResult = imageDownloader.downloadImage(
                    profile.getBody().getKakao_account().getProfile().getProfile_image_url());
            newMember.updateImageUrl(awsS3Uploader.uploadFile("member", newMember.getMemberSeq(),
                    downloadResult.getInputStream(), downloadResult.getContentType(), downloadResult.getFilename()));

            member = newMember;
        }

        if (member.getYnDel().equals("Y")){
            throw new IllegalArgumentException("이미 탈퇴한 회원입니다.");
        }

        if (member.isAdditionalInfoRequired()) {
            String accessToken = jwtTokenProvider.createAtToken(member);
            return MemberLoginResDto.builder()
                    .status(LoginStatus.INCOMPLETE)
                    .accessToken(accessToken)
                    .build();
        }

        String accessToken = jwtTokenProvider.createAtToken(member);
        String refreshToken = jwtTokenProvider.createRtToken(member);
        return MemberLoginResDto.builder()
                .status(LoginStatus.COMPLETE)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public MemberLoginResDto oauthUpdate(OauthUpdateDto oauthUpdateDto) {
        Member member = findMember();
        member.oauthUpdate(oauthUpdateDto);

        String accessToken = jwtTokenProvider.createAtToken(member);
        String refreshToken = jwtTokenProvider.createRtToken(member);
        return MemberLoginResDto.builder()
                .status(LoginStatus.COMPLETE)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Transactional(readOnly = true)
    private Member findMember() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Claims claims = (Claims) SecurityContextHolder.getContext().getAuthentication().getDetails();
        SocialType socialType = SocialType.fromCode(claims.get("socialType", String.class));
        return memberRepository.findByEmailAndSocialType(email, socialType).orElseThrow(() ->
                new EntityNotFoundException("존재하지 않는 회원입니다."));
    }
}
