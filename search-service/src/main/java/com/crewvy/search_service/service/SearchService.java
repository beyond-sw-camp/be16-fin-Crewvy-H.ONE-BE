package com.crewvy.search_service.service;

import com.crewvy.common.event.ApprovalCompletedEvent;
import com.crewvy.common.event.MemberDeletedEvent;
import com.crewvy.common.event.MemberSavedEvent;
import com.crewvy.common.event.OrganizationSavedEvent;
import com.crewvy.search_service.dto.event.MinuteSavedEvent;
import com.crewvy.search_service.dto.response.ApprovalSearchRes;
import com.crewvy.search_service.dto.response.EmployeeSearchRes;
import com.crewvy.search_service.dto.response.SearchResult;
import com.crewvy.search_service.entity.ApprovalDocument;
import com.crewvy.search_service.entity.MemberDocument;
import com.crewvy.search_service.entity.MinuteDocument;
import com.crewvy.search_service.entity.OrganizationDocument;
import com.crewvy.search_service.repository.ApprovalSearchRepository;
import com.crewvy.search_service.repository.MemberSearchRepository;
import com.crewvy.search_service.repository.MinuteRepository;
import com.crewvy.search_service.repository.OrganizationSearchRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHitSupport;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.suggest.Completion;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {
    private final MemberSearchRepository memberSearchRepository;
    private final OrganizationSearchRepository organizationSearchRepository;
    private final ApprovalSearchRepository approvalSearchRepository;
    private final MinuteRepository minuteRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    // 직원 추가
    @KafkaListener(topics = "member-saved-events", groupId = "search-service-group")
    public void listenMemberSavedEvent(MemberSavedEvent event) {
        // 기존 MemberDocument 조회
        memberSearchRepository.findById(event.getMemberId().toString()).ifPresent(existingMember -> {
            List<OrganizationSavedEvent> existingOrgs = existingMember.getOrganizationList();
            List<OrganizationSavedEvent> eventOrgs = event.getOrganizationList();

            // 직원이 제거된 조직 찾기
            existingOrgs.stream()
                    .filter(existingOrg -> eventOrgs.stream().noneMatch(eventOrg -> eventOrg.getOrganizationId().equals(existingOrg.getOrganizationId())))
                    .forEach(removedOrg -> {
                        organizationSearchRepository.findById(removedOrg.getOrganizationId().toString()).ifPresent(orgDoc -> {
                            orgDoc.getMemberList().removeIf(member -> member.getId().equals(event.getMemberId()));
                            organizationSearchRepository.save(orgDoc);
                        });
                    });
        });

        List<String> orgNameList = event.getOrganizationList().stream()
                .map(OrganizationSavedEvent::getName)
                .collect(Collectors.toList());
        String suggestText = event.getName() + " " + String.join(" ", orgNameList);

        MemberDocument memberDocument = MemberDocument.builder()
                .memberId(event.getMemberId().toString())
                .companyId(event.getCompanyId().toString())
                .name(event.getName())
                .organizationList(event.getOrganizationList())
                .titleName(event.getTitleName())
                .phoneNumber(event.getPhoneNumber())
                .email(event.getEmail())
                .memberStatus(event.getMemberStatus())
                .suggest(new Completion(new String[]{suggestText}))
                .build();
        memberSearchRepository.save(memberDocument);

        // 신규 조직이면 추가, 기존 조직이면 member 업데이트
        event.getOrganizationList().forEach(orgEvent -> {
            OrganizationDocument org = organizationSearchRepository.findByCompanyIdAndLabel(event.getCompanyId().toString(), orgEvent.getName())
                    .orElseGet(() -> {
                        OrganizationDocument newOrg = OrganizationDocument.builder()
                                .organizationId(orgEvent.getOrganizationId().toString())
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
    @KafkaListener(topics = "organization-saved-events", groupId = "search-service-group")
    public void listenOrganizationChangedEvent(OrganizationSavedEvent event) {
        OrganizationDocument organizationDocument = organizationSearchRepository.findById(event.getOrganizationId().toString())
                .orElseGet(() ->
                        OrganizationDocument.builder()
                                .organizationId(event.getOrganizationId().toString())
                                .companyId(event.getCompanyId().toString())
                                .memberList(new ArrayList<>())
                                .build());
        organizationDocument.updateFromEvent(event);

        organizationSearchRepository.save(organizationDocument);
    }

    // 직원 삭제
    @KafkaListener(topics = "member-deleted-events", groupId = "search-service-group")
    public void listenMemberDeletedEvent(MemberDeletedEvent event) {
        memberSearchRepository.delete(memberSearchRepository.findById(event.getMemberId().toString()).orElseThrow(
                () -> new EntityNotFoundException("존재하지 않는 직원입니다.")));
    }

    // 결재 문서 저장
    @KafkaListener(topics = "approval-completed-events", groupId = "search-service-group")
    public void listenApprovalCompletedEvent(ApprovalCompletedEvent event) {
        if (event.getApprovalId() == null) {
            log.error("Received ApprovalCompletedEvent with null approvalId. Discarding event: {}", event);
            return;
        }
        ApprovalDocument approvalDocument = ApprovalDocument.builder()
                .approvalId(event.getApprovalId().toString())
                .memberId(event.getMemberId().toString())
                .title(event.getTitle())
                .titleName(event.getTitleName())
                .memberName(event.getMemberName())
                .createAt(event.getCreatedAt())
                .approverIdList(event.getApprovalLineList())
                .build();
        approvalSearchRepository.save(approvalDocument);
    }

    // 회의록 요약 저장
    @KafkaListener(topics = "minute-saved-events", groupId = "search-service-group")
    public void listenMinuteSavedEvent(MinuteSavedEvent event) {
        MinuteDocument minuteDocument = MinuteDocument.builder()
                .minuteId(event.getMinuteId())
                .title(event.getTitle())
                .summary(event.getSummary())
                .memberId(event.getMemberId())
                .createDateTime(event.getCreateDateTime())
                .build();
        minuteRepository.save(minuteDocument);
    }

    // 직원 검색
    public Page<MemberDocument> searchEmployees(String query, String companyId, Pageable pageable) {
        Query searchQuery = buildEmployeeSearchQuery(query, companyId, pageable);
        SearchHits<MemberDocument> searchHits = elasticsearchOperations.search(searchQuery, MemberDocument.class);
        return SearchHitSupport.searchPageFor(searchHits, pageable).map(SearchHit::getContent);
    }

    // 조직 검색
    public List<OrganizationDocument> getOrganizationTree(String companyId) {
        return organizationSearchRepository.findByCompanyIdOrderByDisplayOrderAsc(companyId);
    }

    // 조직별 직원 검색
    public Page<MemberDocument> searchEmployeesByOrganization(String organizationId, UUID companyId, Pageable pageable) {
        Query searchQuery = new NativeQueryBuilder()
                .withQuery(q -> q.bool(b -> b
                        .must(s -> s.nested(n -> n
                                .path("organizationList")
                                .query(nq -> nq.term(t -> t.field("organizationList.organizationId").value(organizationId)))))
                        .filter(f -> f.term(t -> t.field("companyId.keyword").value(companyId.toString())))))
                .withPageable(pageable)
                .build();
        SearchHits<MemberDocument> searchHits = elasticsearchOperations.search(searchQuery, MemberDocument.class);
        return SearchHitSupport.searchPageFor(searchHits, pageable).map(SearchHit::getContent);
    }

    // 결재 문서 페이징 검색
    public Page<ApprovalDocument> searchApprovals(String query, String memberPositionId, Pageable pageable) {
        Query searchQuery = new NativeQueryBuilder()
                .withQuery(q -> q.bool(b -> b
                        .must(m -> m.queryString(qs -> qs.fields("title").query("*".concat(query).concat("*"))))))
                .withPageable(pageable)
                .build();
        SearchHits<ApprovalDocument> searchHits = elasticsearchOperations.search(searchQuery, ApprovalDocument.class);
        return SearchHitSupport.searchPageFor(searchHits, pageable).map(SearchHit::getContent);
    }

    // 통합 검색
    public List<SearchResult> searchGlobal(String query, String companyId, String memberPositionId) {
        Query employeeSearchQuery = buildEmployeeSearchQuery(query, companyId, Pageable.unpaged());

        SearchHits<MemberDocument> employeeHits = elasticsearchOperations.search(employeeSearchQuery, MemberDocument.class);

        List<SearchResult> results = new ArrayList<>();

        // 직원 검색
        employeeHits.forEach(hit -> {
            MemberDocument doc = hit.getContent();
            results.add(EmployeeSearchRes.builder()
                    .id(doc.getMemberId())
                    .category("employee")
                    .title(doc.getName())
                    .department(doc.getOrganizationList() != null ? doc.getOrganizationList().stream()
                            .map(OrganizationSavedEvent::getName).collect(Collectors.joining(", ")) : "")
                    .position(doc.getTitleName() != null ? String.join(", ", doc.getTitleName()) : "")
                    .contact(doc.getPhoneNumber())
                    .email(doc.getEmail())
                    .status(doc.getMemberStatus())
                    .build());
        });

        // 결재문서 검색
        Page<ApprovalDocument> approvalPage = searchApprovals(query, memberPositionId, Pageable.unpaged());
        approvalPage.forEach(doc -> {
            results.add(ApprovalSearchRes.builder()
                    .id(doc.getApprovalId())
                    .category("approval")
                    .title(doc.getTitle())
                    .titleName(doc.getTitleName())
                    .memberName(doc.getMemberName())
                    .createAt(doc.getCreateAt() != null ? doc.getCreateAt().toString() : null)
                    .build());
        });

        return results;
    }

    private Query buildEmployeeSearchQuery(String query, String companyId, Pageable pageable) {
        if (query.matches("^[0-9\\-]+$")) {
            return new NativeQueryBuilder()
                    .withQuery(q -> q.bool(b -> b
                            .must(m -> m.term(t -> t.field("phoneNumber").value(query)))
                            .filter(f -> f.term(t -> t.field("companyId.keyword").value(companyId)))))
                    .withPageable(pageable)
                    .build();
        } else {
            return new NativeQueryBuilder()
                    .withQuery(q -> q.bool(b -> b
                            .should(s -> s.multiMatch(mm -> mm.query(query).fields("name", "titleName", "email")))
                            .should(s -> s.nested(n -> n
                                    .path("organizationList")
                                    .query(nq -> nq.match(m -> m.field("organizationList.name").query(query)))))
                            .minimumShouldMatch("1")
                            .filter(f -> f.term(t -> t.field("companyId.keyword").value(companyId)))))
                    .withPageable(pageable)
                    .build();
        }
    }
}
