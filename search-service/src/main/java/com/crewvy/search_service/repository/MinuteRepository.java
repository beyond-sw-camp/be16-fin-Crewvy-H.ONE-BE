package com.crewvy.search_service.repository;

import com.crewvy.search_service.entity.MinuteDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface MinuteRepository extends ElasticsearchRepository<MinuteDocument, String> {
}
