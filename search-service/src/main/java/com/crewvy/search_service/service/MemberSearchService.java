package com.crewvy.search_service.service;

import com.crewvy.common.event.MemberDeletedEvent;
import com.crewvy.common.event.MemberSavedEvent;
import com.crewvy.search_service.entity.MemberDocument;
import com.crewvy.search_service.repository.ElasticRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberSearchService {

    private final ElasticRepository elasticRepository;

    @KafkaListener(topics = "member-saved-events", groupId = "search-service-group")
    public void listenMemberSavedEvent(MemberSavedEvent event) {
        log.info("Received MemberSavedEvent: {}", event);
        MemberDocument memberDocument = MemberDocument.builder()
                .memberId(event.getMemberId().toString())
                .name(event.getName())
                .phoneNumber(event.getPhoneNumber())
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
}
