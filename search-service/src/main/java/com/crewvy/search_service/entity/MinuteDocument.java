package com.crewvy.search_service.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "minute", createIndex = true)
@Mapping(mappingPath = "elastic/minute-mapping.json")
@Setting(settingPath = "elastic/minute-setting.json")
public class MinuteDocument {
    @Id
    @Field(type = FieldType.Keyword)
    private String minuteId;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text)
    private String summary;

    @Field(type = FieldType.Keyword)
    private List<String> memberId;

    @Field(type = FieldType.Date)
    private LocalDateTime createDateTime;
}
