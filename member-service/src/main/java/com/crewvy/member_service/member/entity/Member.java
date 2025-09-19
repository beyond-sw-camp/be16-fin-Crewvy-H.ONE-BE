package com.crewvy.member_service.member.entity;

import com.chillex.gooseBumps.common.constant.code.member.Role;
import com.chillex.gooseBumps.common.constant.code.member.SocialType;
import com.chillex.gooseBumps.common.constant.entity.YnColumn;
import com.chillex.gooseBumps.common.converter.RoleConverter;
import com.chillex.gooseBumps.common.converter.SocialTypeConverter;
import com.chillex.gooseBumps.domain.BaseEntity;
import com.chillex.gooseBumps.domain.member.dto.request.MemberUpdateDto;
import com.chillex.gooseBumps.domain.member.dto.request.OauthUpdateDto;
import com.chillex.gooseBumps.domain.playlist.entity.PlayList;
import com.chillex.gooseBumps.domain.social.entity.Friend;
import com.chillex.gooseBumps.domain.social.entity.Likes;
import com.chillex.gooseBumps.domain.social.entity.MemberComment;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long memberSeq;
    @Column(nullable = false)
    @Convert(converter = RoleConverter.class)
    @Builder.Default
    private Role roleCode = Role.USER;
    @Column(nullable = false)
    private String email;
    private String password;
    private String name;
    @Column(columnDefinition = "CHAR(13)")
    private String telNo;
    private LocalDate birthDate;
    private String nationalityCode;
    @Column(nullable = false)
    @Builder.Default
    private String ynDel = YnColumn.isFalse;
    private String profileImageUrl;
    private String statusMessage;
    private String nickName;
    @Column(nullable = false)
    @Convert(converter = SocialTypeConverter.class)
    private SocialType socialType;
    @Column(nullable = false)
    private String socialId;
    @Builder.Default
    @OneToMany(mappedBy = "member")
    private List<Friend> friendList = new ArrayList<>();
    @Builder.Default
    @OneToMany(mappedBy = "member", cascade = CascadeType.PERSIST, orphanRemoval = true)
    private List<PlayList> playListList = new ArrayList<>();
    @Builder.Default
    @OneToMany(mappedBy = "member")
    private List<Likes> likesList = new ArrayList<>();
    @Builder.Default
    @OneToMany(mappedBy = "member")
    private List<MemberComment> commentList = new ArrayList<>();

    public void updateMember(MemberUpdateDto memberUpdateDto, String encodeNewPw) {
        this.password = encodeNewPw;
        this.name = memberUpdateDto.getName();
        this.telNo = memberUpdateDto.getTelNo();
        this.nationalityCode = memberUpdateDto.getNationalityCode();
        this.statusMessage = memberUpdateDto.getStatusMessage();
        this.nickName = memberUpdateDto.getNickName();
    }

    public void resetPw(String encodeNewPw){
        this.password = encodeNewPw;
    }

    public void updateImageUrl(String imgUrl){
        this.profileImageUrl = imgUrl;
    }

    public void deleteMember() {
        this.ynDel = YnColumn.isTrue;
    }

    public boolean isAdditionalInfoRequired() {
        return this.telNo == null || this.nationalityCode == null;
    }

    public void oauthUpdate(OauthUpdateDto oauthUpdateDto) {
        this.name = oauthUpdateDto.getName();
        this.nickName = oauthUpdateDto.getNickName();
        this.telNo = oauthUpdateDto.getTelNo();
        this.birthDate = oauthUpdateDto.getBirthDate();
        this.nationalityCode = oauthUpdateDto.getNationalityCode();
    }
}
