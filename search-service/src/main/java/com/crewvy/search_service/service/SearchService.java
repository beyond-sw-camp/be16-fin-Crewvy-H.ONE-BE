package com.crewvy.search_service.service;

import com.crewvy.common.event.MemberDeletedEvent;
import com.crewvy.common.event.MemberSavedEvent;
import com.crewvy.common.event.OrganizationEvent;
import com.crewvy.search_service.entity.MemberDocument;
import com.crewvy.search_service.entity.OrganizationDocument;
import com.crewvy.search_service.repository.MemberSearchRepository;
import com.crewvy.search_service.repository.OrganizationSearchRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.suggest.Completion;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchService {
    private final MemberSearchRepository memberSearchRepository;
    private final OrganizationSearchRepository organizationSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    // 직원 추가
    @KafkaListener(topics = "member-saved-events", groupId = "search-service-group")
    public void listenMemberSavedEvent(MemberSavedEvent event) {
        // 기존 MemberDocument 조회
        memberSearchRepository.findById(event.getMemberId().toString()).ifPresent(existingMember -> {
            List<OrganizationEvent> existingOrgs = existingMember.getOrganizationList();
            List<OrganizationEvent> eventOrgs = event.getOrganizationList();

            // 직원이 제거된 조직 찾기
            existingOrgs.stream()
                    .filter(existingOrg -> eventOrgs.stream().noneMatch(eventOrg -> eventOrg.getId().equals(existingOrg.getId())))
                    .forEach(removedOrg -> {
                        organizationSearchRepository.findById(removedOrg.getId().toString()).ifPresent(orgDoc -> {
                            orgDoc.getMemberList().removeIf(member -> member.getId().equals(event.getMemberId()));
                            organizationSearchRepository.save(orgDoc);
                        });
                    });
        });

        List<String> orgNameList = event.getOrganizationList().stream()
                .map(OrganizationEvent::getName)
                .collect(Collectors.toList());
        String suggestText = event.getName() + " " + String.join(" ", orgNameList);

        MemberDocument memberDocument = MemberDocument.builder()
                .memberId(event.getMemberId().toString())
                .companyId(event.getCompanyId().toString())
                .name(event.getName())
                .organizationList(event.getOrganizationList())
                .titleName(event.getTitleName())
                .phoneNumber(event.getPhoneNumber())
                .memberStatus(event.getMemberStatus())
                .suggest(new Completion(new String[]{suggestText}))
                .build();
        memberSearchRepository.save(memberDocument);

        // 신규 조직이면 추가, 기존 조직이면 member 업데이트
        event.getOrganizationList().forEach(orgEvent -> {
            OrganizationDocument org = organizationSearchRepository.findByCompanyIdAndLabel(event.getCompanyId().toString(), orgEvent.getName())
                    .orElseGet(() -> {
                        OrganizationDocument newOrg = OrganizationDocument.builder()
                                .id(orgEvent.getId().toString())
                                .companyId(event.getCompanyId().toString())
                                .label(orgEvent.getName())
                                .memberList(new ArrayList<>())
                                .parentId(orgEvent.getParentId() != null ? orgEvent.getParentId().toString() : null)
                                .build();
                        return organizationSearchRepository.save(newOrg);
                    });

            OrganizationDocument.Member member = org.getMemberList().stream()
                    .filter(m -> m.getId().equals(event.getMemberId()))
                    .findFirst()
                    .orElse(new OrganizationDocument.Member());

            member.updateMember(event);

            if (org.getMemberList().stream().noneMatch(m -> m.getId().equals(event.getMemberId()))) {
                org.getMemberList().add(member);
            }

            organizationSearchRepository.save(org);
        });
    }

        // 조직 추가/수정

        @KafkaListener(topics = "organization-changed-events", groupId = "search-service-group")

        public void listenOrganizationChangedEvent(OrganizationEvent event) {

            OrganizationDocument organizationDocument = OrganizationDocument.builder()

                    .id(event.getId().toString())

                    .companyId(event.getCompanyId().toString())

                    .label(event.getName())

                    .parentId(event.getParentId() != null ? event.getParentId().toString() : null)

                    .memberList(new ArrayList<>())

                    .build();

            organizationSearchRepository.save(organizationDocument);

        }

    

        // 직원 삭제

        @KafkaListener(topics = "member-deleted-events", groupId = "search-service-group")

        public void listenMemberDeletedEvent(MemberDeletedEvent event) {

            memberSearchRepository.delete(memberSearchRepository.findById(event.getMemberId().toString()).orElseThrow(

                    () -> new EntityNotFoundException("존재하지 않는 직원입니다.")));

        }

    // 직원 검색
    public List<MemberDocument> searchEmployees(String query, String companyId) {
        Query searchQuery;
        if (query.matches("^[0-9\\-]+$")) {
            searchQuery = new NativeQueryBuilder()
                    .withQuery(q -> q.bool(b -> b
                            .must(m -> m.term(t -> t.field("phoneNumber").value(query)))
                            .filter(f -> f.term(t -> t.field("companyId.keyword").value(companyId)))))
                    .build();
        } else {
            searchQuery = new NativeQueryBuilder()
                    .withQuery(q -> q.bool(b -> b
                            .should(s -> s.multiMatch(mm -> mm.query(query).fields("name")))
                            .should(s -> s.nested(n -> n
                                    .path("organizationList")
                                    .query(nq -> nq.match(m -> m.field("organizationList.name").query(query)))))
                            .minimumShouldMatch("1")
                            .filter(f -> f.term(t -> t.field("companyId.keyword").value(companyId)))))
                    .build();
        }
        SearchHits<MemberDocument> searchHits = elasticsearchOperations.search(searchQuery, MemberDocument.class);
        return searchHits.stream().map(SearchHit::getContent).collect(Collectors.toList());
    }

    // 조직 검색
    public List<OrganizationDocument> getOrganizationTree(String companyId) {
        return organizationSearchRepository.findByCompanyId(companyId);
    }

    // 조직별 직원 검색
    public List<MemberDocument> searchEmployeesByOrganization(String organizationId, String companyId) {
        Query searchQuery = new NativeQueryBuilder()
                .withQuery(q -> q.bool(b -> b
                        .must(s -> s.nested(n -> n
                                .path("organizationList")
                                .query(nq -> nq.term(t -> t.field("organizationList.id").value(organizationId)))))
                        .filter(f -> f.term(t -> t.field("companyId.keyword").value(companyId)))))
                .build();
        SearchHits<MemberDocument> searchHits = elasticsearchOperations.search(searchQuery, MemberDocument.class);
        return searchHits.stream().map(SearchHit::getContent).collect(Collectors.toList());
    }
}
