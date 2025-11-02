package com.crewvy.search_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Mapping;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(indexName = "approval", createIndex = true)
@Setting(settingPath = "elastic/approval-setting.json")
@Mapping(mappingPath = "elastic/approval-mapping.json")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApprovalDocument {
    @Id
    @Field(type = FieldType.Keyword)
    private String approvalId;

    @Field(type = FieldType.Keyword)
    private String companyId;

    @Field(type = FieldType.Keyword)
    private String memberId;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Date)
    private LocalDateTime createDateTime;
}
