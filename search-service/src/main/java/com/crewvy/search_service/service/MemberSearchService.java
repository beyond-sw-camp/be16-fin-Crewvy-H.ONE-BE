package com.crewvy.search_service.service;

import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import com.crewvy.common.event.MemberDeletedEvent;
import com.crewvy.common.event.MemberSavedEvent;
import com.crewvy.search_service.entity.MemberDocument;
import com.crewvy.search_service.repository.ElasticRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberSearchService {

    private final ElasticRepository elasticRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    @KafkaListener(topics = "member-saved-events", groupId = "search-service-group")
    public void listenMemberSavedEvent(MemberSavedEvent event) {
        log.info("Received MemberSavedEvent: {}", event);
        MemberDocument memberDocument = MemberDocument.builder()
                .memberId(event.getMemberId().toString())
                .name(event.getName())
                .organizationName(event.getOrganizationName())
                .titleName(event.getTitleName())
                .phoneNumber(event.getPhoneNumber())
                .memberStatus(event.getMemberStatus())
                .suggest(event.getName() + " " + String.join(" ", event.getOrganizationName()))
                .build();
        elasticRepository.save(memberDocument);
        log.info("Saved MemberDocument to Elasticsearch: {}", memberDocument.getMemberId());
    }

    @KafkaListener(topics = "member-deleted-events", groupId = "search-service-group")
    public void listenMemberDeletedEvent(MemberDeletedEvent event) {
        log.info("Received MemberDeletedEvent: {}", event);
        elasticRepository.deleteById(event.getMemberId().toString());
        log.info("Deleted MemberDocument from Elasticsearch: {}", event.getMemberId());
    }

    public List<MemberDocument> unifiedSearch(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("검색어를 입력해 주세요.");
        }

        Query searchQuery;
        if (keyword.matches("^[0-9\\-]+$")) {
            searchQuery = new NativeQueryBuilder()
                    .withQuery(qb -> qb.term(t -> t.field("phoneNumber").value(keyword)))
                    .build();
        } else {
            searchQuery = new NativeQueryBuilder()
                    .withQuery(qb -> qb.multiMatch(mm -> mm.query(keyword).fields("name", "organizationName")))
                    .build();
        }
        SearchHits<MemberDocument> searchHits = elasticsearchOperations.search(searchQuery, MemberDocument.class);
        return searchHits.stream().map(SearchHit::getContent).collect(Collectors.toList());
    }

    public List<String> getAutocompleteSuggestions(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("검색어를 입력해 주세요.");
        }

        Query searchQuery = new NativeQueryBuilder()
                .withQuery(qb -> qb.match(m -> m.field("suggest").query(keyword).operator(Operator.And)))
                .withPageable(PageRequest.of(0, 10))
                .build();
        SearchHits<MemberDocument> searchHits = elasticsearchOperations.search(searchQuery, MemberDocument.class);
        return searchHits.stream()
                .map(SearchHit::getContent)
                .map(MemberDocument::getSuggest)
                .distinct()
                .collect(Collectors.toList());
    }

    public List<MemberDocument> searchEmployees(String query) {
        Query searchQuery;
        if (query.matches("^[0-9\\-]+$")) { // Check if query looks like a phone number
            searchQuery = new NativeQueryBuilder()
                    .withQuery(qb -> qb.term(t -> t.field("phoneNumber").value(query)))
                    .build();
        } else {
            searchQuery = new NativeQueryBuilder()
                    .withQuery(qb -> qb.multiMatch(mm -> mm.query(query).fields("name", "organizationName")))
                    .build();
        }
        SearchHits<MemberDocument> searchHits = elasticsearchOperations.search(searchQuery, MemberDocument.class);
        return searchHits.stream().map(SearchHit::getContent).collect(Collectors.toList());
    }
}
