package com.crewvy.member_service.member.dto.response;

import com.crewvy.member_service.member.entity.Title;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TitleRes {
    private UUID id;
    private String name;
    private boolean ynDel;

    public static TitleRes fromEntity(Title title) {
        return TitleRes.builder()
                .id(title.getId())
                .name(title.getName())
                .ynDel(title.getYnDel().toBoolean())
                .build();
    }
}
