package com.crewvy.search_service.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private String videoConferenceId;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Text)
    private String summary;

    @Field(type = FieldType.Keyword)
    private String hostId;

    @Field(type = FieldType.Keyword)
    @Builder.Default
    private Set<String> inviteeIdSet = new HashSet<>();

    @Field(type = FieldType.Date)
    private LocalDateTime createAt;
}
