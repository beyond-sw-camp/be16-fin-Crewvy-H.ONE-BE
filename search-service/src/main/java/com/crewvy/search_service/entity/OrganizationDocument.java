package com.crewvy.search_service.entity;

import com.crewvy.common.event.MemberSavedEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Mapping;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "organization", createIndex = true)
@Mapping(mappingPath = "elastic/organization-mapping.json")
public class OrganizationDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword, name = "companyId")
    private String companyId;

    @Field(type = FieldType.Keyword)
    private String label;

    @Field(type = FieldType.Keyword)
    private String type;

    @Field(type = FieldType.Nested)
    private List<Member> memberList;

    @Field(type = FieldType.Keyword)
    private String parentId;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Member {
        @Field(type = FieldType.Keyword)
        private UUID id;

        @Field(type = FieldType.Text)
        private String name;

        @Field(type = FieldType.Keyword)
        private String position;

        @Field(type = FieldType.Keyword)
        private String email;

        public void updateMember(MemberSavedEvent event){
            this.id = event.getMemberId();
            this.name = event.getName();
            this.position = event.getPosition();
            this.email = event.getEmail();
        }
    }
}
