package com.crewvy.search_service.repository;

import com.crewvy.search_service.entity.OrganizationDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;
import java.util.Optional;

public interface OrganizationSearchRepository extends ElasticsearchRepository<OrganizationDocument, String> {
    List<OrganizationDocument> findByCompanyId(String companyId);
    Optional<OrganizationDocument> findByCompanyIdAndLabel(String companyId, String label);
}
