package com.crewvy.member_service.member.dto.response;

import com.chillex.gooseBumps.common.constant.code.member.Role;
import com.chillex.gooseBumps.domain.member.entity.Member;
import com.neovisionaries.i18n.CountryCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Locale;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class MemberListDto {
    private Long memberSeq;
    private Role roleCode;
    private String email;
    private String name;
    private String telNo;
    private LocalDate birthDate;
    private String nationalityCode;
    private String nickName;
    private String socialType;
    private String socialId;

    public static MemberListDto fromEntity(Member member){
        return MemberListDto.builder()
                .memberSeq(member.getMemberSeq())
                .roleCode(member.getRoleCode())
                .email(member.getEmail())
                .name(member.getName())
                .telNo(member.getTelNo())
                .birthDate(member.getBirthDate())
                .nationalityCode(CountryCode.getByCode(Integer.parseInt(member.getNationalityCode())).toLocale().getDisplayCountry(Locale.KOREAN))
                .nickName(member.getNickName())
                .socialType(member.getSocialType().getCodeName())
                .socialId(member.getSocialId())
                .build();
    }
}
