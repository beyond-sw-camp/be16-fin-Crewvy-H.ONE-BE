package com.crewvy.member_service.member.entity;

import com.crewvy.member_service.common.constant.MemberStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID memberId;

    @Column(nullable = false, unique = true)
    private String email;

    private String password;

    private String name;

    private String phoneNumber;

    @Column(nullable = false)
    private boolean isPhoneNumberPublic;

    private String address;

    @Column(nullable = false)
    private boolean isAddressDisclosure;

    private String sabun;

    private String bank;

    private String bankAccount;

    private String profileUrl;

    @Column(nullable = false)
    private MemberStatus memberStatus;

    @Column(nullable = false)
    private String ynDel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT), nullable = false)
    private Company company;
}
