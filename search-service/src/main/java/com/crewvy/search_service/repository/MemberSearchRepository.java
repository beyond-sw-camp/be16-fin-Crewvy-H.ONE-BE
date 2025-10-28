package com.crewvy.search_service.repository;

import com.crewvy.search_service.entity.MemberDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface MemberSearchRepository extends ElasticsearchRepository<MemberDocument, String> {
}
