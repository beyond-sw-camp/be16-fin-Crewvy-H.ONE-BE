package com.crewvy.search_service.repository;

import com.crewvy.search_service.entity.ApprovalDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ApprovalSearchRepository extends ElasticsearchRepository<ApprovalDocument, String> {
    Page<ApprovalDocument> findByTitleContainingAndCompanyId(String title, String companyId, Pageable pageable);
}
