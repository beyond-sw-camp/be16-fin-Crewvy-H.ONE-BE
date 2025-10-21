package com.crewvy.search_service.entity;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class MemberDocument {
    @Id
    @Field(type = FieldType.Keyword)
    private String memberId;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Text)
    private String phoneNumber;

    // Note: The 'Member' entity is not available in search-service. 
    // You will need to decide how MemberDocument is created/updated in search-service.
    // This 'fromEntity' method might need to be removed or adapted.
    // public static MemberDocument fromEntity(Member member){
    //     return MemberDocument.builder()
    //             .memberId(member.getId().toString())
    //             .name(member.getName())
    //             .phoneNumber(member.getPhoneNumber())
    //             .build();
    // }
}
