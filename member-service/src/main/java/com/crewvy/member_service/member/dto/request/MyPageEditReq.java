package com.crewvy.member_service.member.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MyPageEditReq {
    private String profileUrl;
    private String phoneNumber;
    private Boolean isPhoneNumberPublic;
    private String emergencyContact;
    private String extensionNumber;
    private String telNumber;
    private String address;
    private Boolean isAddressDisclosure;
    private String bank;
    private String bankAccount;
    private String currentPassword;
    private String newPassword;
    private String confirmPassword;
}
