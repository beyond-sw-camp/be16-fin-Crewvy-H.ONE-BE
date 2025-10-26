package com.crewvy.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberSavedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID memberId;
    private String name;
    private UUID companyId;
    private List<OrganizationEvent> organizationList;
    private List<String> titleName;
    private String phoneNumber;
    private String memberStatus;
    private String position;
    private String email;
}
