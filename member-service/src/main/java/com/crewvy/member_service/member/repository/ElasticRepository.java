package com.crewvy.member_service.member.repository;

import com.crewvy.member_service.member.entity.MemberDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ElasticRepository extends ElasticsearchRepository<MemberDocument, String> {
}
