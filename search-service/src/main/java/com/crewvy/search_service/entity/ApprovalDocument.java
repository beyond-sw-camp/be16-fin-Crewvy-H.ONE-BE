package com.crewvy.search_service.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Mapping;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "approval", createIndex = true)
@Mapping(mappingPath = "elastic/approval-mapping.json")
public class ApprovalDocument {
    @Id
    @Field(type = FieldType.Keyword)
    private String approvalId;

    @Field(type = FieldType.Keyword)
    private String memberId;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Date)
    private LocalDateTime createDateTime;
}
