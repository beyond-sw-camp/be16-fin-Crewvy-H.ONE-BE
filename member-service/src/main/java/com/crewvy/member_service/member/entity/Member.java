package com.crewvy.member_service.member.entity;

import com.crewvy.common.entity.BaseEntity;
import com.crewvy.common.entity.Bool;
import com.crewvy.member_service.member.constant.AccountStatus;
import com.crewvy.member_service.member.constant.EmploymentType;
import com.crewvy.member_service.member.constant.MemberStatus;
import com.crewvy.member_service.member.converter.AccountStatusConverter;
import com.crewvy.member_service.member.converter.EmploymentTypeConverter;
import com.crewvy.member_service.member.converter.MemberStatusConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    private String password;

    private String name;

    private String phoneNumber;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Bool isPhoneNumberPublic = Bool.TRUE;

    private String emergencyContact;

    private String address;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Bool isAddressDisclosure = Bool.TRUE;

    private String bank;

    private String bankAccount;

    private String profileUrl;

    private String sabun;

    private LocalDate joinDate;

    private String extensionNumber; // 내선번호

    private String telNumber;       // 일반전화

    @Column(nullable = false)
    @Convert(converter = MemberStatusConverter.class)
    @Builder.Default
    private MemberStatus memberStatus = MemberStatus.WORKING;

    @Column(nullable = false)
    @Convert(converter = AccountStatusConverter.class)
    @Builder.Default
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Column(nullable = false)
    @Convert(converter = EmploymentTypeConverter.class)
    private EmploymentType employmentType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Bool ynDel = Bool.FALSE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT), nullable = false)
    private Company company;

    @Builder.Default
    @OneToMany(mappedBy = "member", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    private Set<MemberPosition> memberPositionList = new LinkedHashSet<>();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_member_position_id")
    private MemberPosition defaultMemberPosition;

    @Builder.Default
    @OneToMany(mappedBy = "member", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    private Set<GradeHistory> gradeHistorySet = new LinkedHashSet<>();

    public void updateDefaultMemberPosition(MemberPosition newMemberPosition){
        this.defaultMemberPosition = newMemberPosition;
    }
}
