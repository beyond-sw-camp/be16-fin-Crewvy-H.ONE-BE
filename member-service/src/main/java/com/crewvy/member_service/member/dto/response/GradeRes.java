package com.crewvy.member_service.member.dto.response;

import com.crewvy.member_service.member.entity.Grade;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradeRes {
    private UUID id;
    private String name;

    public static GradeRes fromEntity(Grade grade) {
        return GradeRes.builder()
                .id(grade.getId())
                .name(grade.getName())
                .build();
    }
}
