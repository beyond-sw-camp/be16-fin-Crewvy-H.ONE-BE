package com.crewvy.member_service.member.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(indexName = "members", createIndex = true)
@Setting(settingPath = "elastic/members-setting.json")
@Mapping(mappingPath = "elastic/members-mapping.json")
public class MemberDocument {
    @Id
    @Field(type = FieldType.Keyword)
    private String memberId;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Text)
    private String phoneNumber;

    public static MemberDocument fromEntity(Member member){
        return MemberDocument.builder()
                .memberId(member.getId().toString())
                .name(member.getName())
                .phoneNumber(member.getPhoneNumber())
                .build();
    }
}
