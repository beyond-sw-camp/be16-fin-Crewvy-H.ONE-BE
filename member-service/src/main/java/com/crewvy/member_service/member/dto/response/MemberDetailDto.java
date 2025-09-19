package com.crewvy.member_service.member.dto.response;

import com.chillex.gooseBumps.common.constant.code.member.Role;
import com.chillex.gooseBumps.domain.member.entity.Member;
import com.chillex.gooseBumps.domain.playlist.dto.response.PlayListDto;
import com.neovisionaries.i18n.CountryCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class MemberDetailDto {
    private Long memberSeq;
    private Role roleCode;
    private String email;
    private String name;
    private String telNo;
    private LocalDate birthDate;
    private String nationalityCode;
    private String ynDel;
    private String profileImageUrl;
    private String statusMessage;
    private String nickName;
    private String socialType;
    private String socialId;
//    private List<Friend> friendList;
    private List<PlayListDto> playListDtos;
//    private List<Likes> likesList;

    public static MemberDetailDto fromEntity(Member member, List<PlayListDto> playListDtos){
        String nationality = null;
        if (member.getNationalityCode() != null) {
            nationality = CountryCode.getByCode(Integer.parseInt(member.getNationalityCode())).toLocale().getDisplayCountry(Locale.KOREAN);
        }

        return MemberDetailDto.builder()
                .memberSeq(member.getMemberSeq())
                .roleCode(member.getRoleCode())
                .email(member.getEmail())
                .name(member.getName())
                .telNo(member.getTelNo())
                .birthDate(member.getBirthDate())
                .nationalityCode(nationality)
                .ynDel(member.getYnDel())
                .profileImageUrl(member.getProfileImageUrl())
                .statusMessage(member.getStatusMessage())
                .nickName(member.getNickName())
                .socialType(member.getSocialType().getCodeName())
                .socialId(member.getSocialId())
//                .friendList(member.getFriendList())
                .playListDtos(playListDtos)
//                .likesList(member.getLikesList())
                .build();
    }
}
