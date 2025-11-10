package com.crewvy.search_service.repository;

import com.crewvy.search_service.entity.MinuteDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface MinuteSearchRepository extends ElasticsearchRepository<MinuteDocument, String> {
}
