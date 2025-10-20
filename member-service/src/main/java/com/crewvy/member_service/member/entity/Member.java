package com.crewvy.member_service.member.entity;

import com.crewvy.common.entity.BaseEntity;
import com.crewvy.common.entity.Bool;
import com.crewvy.member_service.member.constant.AccountStatus;
import com.crewvy.member_service.member.constant.EmploymentType;
import com.crewvy.member_service.member.constant.MemberStatus;
import com.crewvy.member_service.member.converter.AccountStatusConverter;
import com.crewvy.member_service.member.converter.EmploymentTypeConverter;
import com.crewvy.member_service.member.converter.MemberStatusConverter;
import com.crewvy.member_service.member.dto.request.MyPageEditReq;
import com.crewvy.member_service.member.dto.request.UpdateMemberReq;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
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

    private String extensionNumber; // 내선전화

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

    public void updateBasicInfo(UpdateMemberReq updateMemberReq, String encodePw) {
        this.name = updateMemberReq.getName();
        this.password = encodePw;
        this.joinDate = updateMemberReq.getJoinDate();
        this.extensionNumber = updateMemberReq.getExtensionNumber();
        this.telNumber = updateMemberReq.getTelNumber();

        if (updateMemberReq.getAccountStatusCodeValue() != null && !updateMemberReq.getAccountStatusCodeValue().isBlank()) {
            this.accountStatus = AccountStatus.fromCode(updateMemberReq.getAccountStatusCodeValue());
        }
        if (updateMemberReq.getEmploymentTypeCodeValue() != null && !updateMemberReq.getEmploymentTypeCodeValue().isBlank()) {
            this.employmentType = EmploymentType.fromCode(updateMemberReq.getEmploymentTypeCodeValue());
        }
        if (updateMemberReq.getMemberStatusCodeValue() != null && !updateMemberReq.getMemberStatusCodeValue().isBlank()) {
            this.memberStatus = MemberStatus.fromCode(updateMemberReq.getMemberStatusCodeValue());
        }
    }

    public void updateMyPage(MyPageEditReq myPageEditReq, String encodePw){
        this.phoneNumber = myPageEditReq.getPhoneNumber();
        this.emergencyContact = myPageEditReq.getEmergencyContact();
        this.extensionNumber = myPageEditReq.getExtensionNumber();
        this.telNumber = myPageEditReq.getTelNumber();
        this.address = myPageEditReq.getAddress();
        if (myPageEditReq.getIsPhoneNumberPublic() != null) {
            this.isPhoneNumberPublic = Bool.fromBoolean(myPageEditReq.getIsPhoneNumberPublic());
        }
        if (myPageEditReq.getIsAddressDisclosure() != null) {
            this.isAddressDisclosure = Bool.fromBoolean(myPageEditReq.getIsAddressDisclosure());
        }
        this.bank = myPageEditReq.getBank();
        this.bankAccount = myPageEditReq.getBankAccount();
        this.password = encodePw;
    }

    public void delete() {
        this.ynDel = Bool.TRUE;
        this.memberStatus = MemberStatus.DELETED;
    }

    public void restore() {
        this.ynDel = Bool.FALSE;
        this.memberStatus = MemberStatus.WORKING;
    }
}
