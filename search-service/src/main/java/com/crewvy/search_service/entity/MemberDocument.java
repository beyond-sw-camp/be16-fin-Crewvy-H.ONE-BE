package com.crewvy.search_service.entity;


import com.crewvy.common.event.OrganizationSavedEvent;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import org.springframework.data.elasticsearch.core.suggest.Completion;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(indexName = "member", createIndex = true)
@Setting(settingPath = "elastic/member-setting.json")
@Mapping(mappingPath = "elastic/member-mapping.json")
@JsonIgnoreProperties(ignoreUnknown = true)
public class MemberDocument {
    @Id
    @Field(type = FieldType.Keyword)
    private String memberId;

    @Field(type = FieldType.Keyword, name = "companyId")
    private String companyId;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Nested)
    private List<OrganizationSavedEvent> organizationList;

    @Field(type = FieldType.Text)
    private List<String> titleName;

    @Field(type = FieldType.Keyword, name = "phoneNumber")
    private String phoneNumber;

    @Field(type = FieldType.Text)
    private String memberStatus;

    @CompletionField(maxInputLength = 100)
    private Completion suggest;
}
