package com.crewvy.search_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(indexName = "approval", createIndex = false, writeTypeHint = WriteTypeHint.FALSE)
@Setting(settingPath = "elastic/approval-setting.json")
@Mapping(mappingPath = "elastic/approval-mapping.json")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApprovalDocument {
    @Id
    @Field(type = FieldType.Keyword)
    private String approvalId;

    @Field(type = FieldType.Keyword)
    private String memberPositionId;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text)
    private String titleName;

    @Field(type = FieldType.Text)
    private String memberName;

    @Field(type = FieldType.Keyword)
    private List<String> approverIdList;

    @Field(type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS||epoch_millis")
    private LocalDateTime createAt;
}
