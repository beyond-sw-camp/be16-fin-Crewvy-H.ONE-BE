package com.crewvy.member_service.member.dto.request;

import com.crewvy.member_service.member.constant.Action;
import com.crewvy.member_service.member.entity.Permission;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreatePermissionReq {
    @NotEmpty(message = "권한의 이름을 입력해 주세요.")
    private String name;
    @NotEmpty(message = "자원을 입력해 주세요.")
    private String resource;
    @NotEmpty(message = "작업을 선택해 주세요.")
    private String action;
    private String description;

    public Permission toEntity(){
        return Permission.builder()
                .name(this.name)
                .resource(this.resource)
                .action(Action.fromCode(this.action))
                .description(this.description)
                .build();
    }
}
