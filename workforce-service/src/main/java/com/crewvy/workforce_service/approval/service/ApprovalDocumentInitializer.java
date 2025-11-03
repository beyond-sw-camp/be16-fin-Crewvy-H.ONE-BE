package com.crewvy.workforce_service.approval.service;

import com.crewvy.workforce_service.approval.entity.ApprovalDocument;
import com.crewvy.workforce_service.approval.repository.ApprovalDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList; // ArrayList import 추가
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalDocumentInitializer implements ApplicationRunner {

    private final ApprovalDocumentRepository documentRepository;
    private final Environment environment;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        log.info("=== 기본 결재 문서 데이터 생성 시작 ===");

        String ddlAuto = environment.getProperty("spring.jpa.hibernate.ddl-auto");
        if (!("create".equals(ddlAuto) || "create-drop".equals(ddlAuto))) {
            log.info("=== ddl-auto가 create/create-drop가 아니므로 건너<0xEB><0x9C><0x90>니다. ===");
            return;
        }

        if (documentRepository.count() == 0) {

            // --- 1. 휴가 신청서 ---
            Map<String, Object> leaveMetadata = createLeaveRequestMetadata(); // 메서드로 분리
            ApprovalDocument leaveDoc = ApprovalDocument.builder()
                    .documentName("휴가 신청서")
                    .metadata(leaveMetadata)
                    .build();
            documentRepository.save(leaveDoc);
            log.info("기본 문서 생성: {}", leaveDoc.getDocumentName());

            // --- 2. 출장 신청서 ---
            Map<String, Object> tripMetadata = createBusinessTripMetadata(); // 메서드로 분리
            ApprovalDocument tripDoc = ApprovalDocument.builder()
                    .documentName("출장 신청서")
                    .metadata(tripMetadata)
                    .build();
            documentRepository.save(tripDoc);
            log.info("기본 문서 생성: {}", tripDoc.getDocumentName());

            // --- 3. 휴직 신청서 ---
            Map<String, Object> leaveAbsenceMetadata = createLeaveOfAbsenceMetadata(); // 메서드로 분리
            ApprovalDocument leaveAbsenceDoc = ApprovalDocument.builder()
                    .documentName("휴직 신청서")
                    .metadata(leaveAbsenceMetadata)
                    .build();
            documentRepository.save(leaveAbsenceDoc);
            log.info("기본 문서 생성: {}", leaveAbsenceDoc.getDocumentName());

            // --- (다른 기본 문서들 추가) ---

            log.info("=== 기본 결재 문서 생성 완료 ===");
        } else {
            log.info("=== 이미 결재 문서가 존재하여 건너<0xEB><0x9C><0x90>니다. ===");
        }
    }

    // --- Metadata 생성 헬퍼 메서드들 ---

    private Map<String, Object> createLeaveRequestMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("template_name", "휴가 신청서");
        metadata.put("description", "연차, 반차, 병가 등 개인 휴가를 신청하는 양식입니다.");

        Map<String, Object> schema = new HashMap<>();
        List<List<Map<String, Object>>> rows = new ArrayList<>();

        // Row 1: 기본 정보 (읽기 전용)
        rows.add(List.of(
                Map.of("id", "department", "label", "소속", "type", "text", "readonly", true),
                Map.of("id", "position", "label", "직급", "type", "text", "readonly", true),
                Map.of("id", "name", "label", "신청자", "type", "text", "readonly", true)
        ));
        // Row 2: 휴가 종류
        rows.add(List.of(
                Map.of("id", "leave_type", "label", "휴가 종류", "type", "select", "required", true,
                        "options", List.of("연차", "오전 반차", "오후 반차", "병가", "경조사 휴가", "공가"))
        ));
        // Row 3: 휴가 기간
        rows.add(List.of(
                Map.of("id", "start_date", "label", "시작일", "type", "date", "required", true),
                Map.of("id", "end_date", "label", "종료일", "type", "date", "required", true)
        ));
        // Row 4: 사유
        rows.add(List.of(
                Map.of("id", "reason", "label", "휴가 사유", "type", "textarea", "required", true,
                        "placeholder", "예: 가족 여행, 개인 사유")
        ));
        // Row 5: 비상 연락처
        rows.add(List.of(
                Map.of("id", "emergency_contact", "label", "비상 연락처", "type", "tel", "required", true,
                        "placeholder", "휴가 중 연락 가능한 번호")
        ));
        // Row 6: 업무 대리인
        rows.add(List.of(
                Map.of("id", "handover_agent", "label", "업무 대리인", "type", "text", "required", true,
                        "placeholder", "대리인의 이름과 소속을 기재")
        ));
        // Row 7: 인수인계 내용
        rows.add(List.of(
                Map.of("id", "handover_details", "label", "인수인계 내용", "type", "textarea", "required", true,
                        "placeholder", "부재 중 처리해야 할 업무 내용을 상세히 기재")
        ));

        schema.put("rows", rows);
        metadata.put("schema", schema);
        return metadata;
    }

    private Map<String, Object> createBusinessTripMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("template_name", "출장 신청서");
        metadata.put("description", "업무상 출장 시, 목적과 경비를 사전에 승인받기 위한 양식입니다.");

        Map<String, Object> schema = new HashMap<>();
        List<List<Map<String, Object>>> rows = new ArrayList<>();

        // Row 1: 기본 정보
        rows.add(List.of(
                Map.of("id", "department", "label", "소속", "type", "text", "readonly", true),
                Map.of("id", "position", "label", "직급", "type", "text", "readonly", true),
                Map.of("id", "name", "label", "신청자", "type", "text", "readonly", true)
        ));
        // Row 2: 출장지
        rows.add(List.of(
                Map.of("id", "destination", "label", "출장지", "type", "text", "required", true,
                        "placeholder", "예: 부산광역시 해운대구")
        ));
        // Row 3: 출장 기간
        rows.add(List.of(
                Map.of("id", "start_date", "label", "출장 시작일", "type", "date", "required", true),
                Map.of("id", "end_date", "label", "출장 종료일", "type", "date", "required", true)
        ));
        // Row 4: 출장 목적
        rows.add(List.of(
                Map.of("id", "purpose", "label", "출장 목적", "type", "textarea", "required", true,
                        "placeholder", "방문 기관, 미팅 대상, 주요 업무 내용을 상세히 기재")
        ));
        // Row 5: 예상 경비
        rows.add(List.of(
                Map.of("id", "transportation_cost", "label", "예상 교통비", "type", "number", "required", false, "placeholder", "단위: 원"),
                Map.of("id", "accommodation_cost", "label", "예상 숙박비", "type", "number", "required", false, "placeholder", "단위: 원"),
                Map.of("id", "other_cost", "label", "예상 기타 경비", "type", "number", "required", false, "placeholder", "단위: 원")
        ));
        // Row 6: 업무 대리인
        rows.add(List.of(
                Map.of("id", "handover_agent", "label", "업무 대리인", "type", "text", "required", true,
                        "placeholder", "대리인의 이름과 소속을 기재")
        ));
        // Row 7: 인수인계 내용
        rows.add(List.of(
                Map.of("id", "handover_details", "label", "인수인계 내용", "type", "textarea", "required", true,
                        "placeholder", "부재 중 처리해야 할 업무 내용을 상세히 기재")
        ));

        schema.put("rows", rows);
        metadata.put("schema", schema);
        return metadata;
    }

    private Map<String, Object> createLeaveOfAbsenceMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("template_name", "휴직 신청서");
        metadata.put("description", "질병, 육아, 자기계발 등 개인 사유로 인한 휴직을 신청하는 양식입니다.");

        Map<String, Object> schema = new HashMap<>();
        List<List<Map<String, Object>>> rows = new ArrayList<>();

        // Row 1: 기본 정보
        rows.add(List.of(
                Map.of("id", "department", "label", "소속", "type", "text", "readonly", true),
                Map.of("id", "position", "label", "직급", "type", "text", "readonly", true),
                Map.of("id", "name", "label", "신청자", "type", "text", "readonly", true)
        ));
        // Row 2: 휴직 종류
        rows.add(List.of(
                Map.of("id", "leave_of_absence_type", "label", "휴직 종류", "type", "select", "required", true,
                        "options", List.of("질병 휴직", "육아 휴직", "가족돌봄 휴직", "자기계발 휴직", "무급 휴직"))
        ));
        // Row 3: 휴직 기간
        rows.add(List.of(
                Map.of("id", "start_date", "label", "휴직 시작일", "type", "date", "required", true),
                Map.of("id", "end_date", "label", "휴직 종료일", "type", "date", "required", true)
        ));
        // Row 4: 사유
        rows.add(List.of(
                Map.of("id", "reason", "label", "휴직 사유", "type", "textarea", "required", true,
                        "placeholder", "휴직 사유를 상세히 기재 (필요시 증빙 서류 제출)")
        ));
        // Row 5: 연락처
        rows.add(List.of(
                Map.of("id", "emergency_contact", "label", "휴직 중 연락처", "type", "tel", "required", true,
                        "placeholder", "휴직 기간 중 연락 가능한 번호")
        ));
        // Row 6: 업무 대리인
        rows.add(List.of(
                Map.of("id", "handover_agent", "label", "업무 대리인", "type", "text", "required", true,
                        "placeholder", "대리인의 이름과 소속을 기재")
        ));
        // Row 7: 인수인계 내용
        rows.add(List.of(
                Map.of("id", "handover_details", "label", "인수인계 내용", "type", "textarea", "required", true,
                        "placeholder", "휴직 전 인수인계 할 업무 내용을 상세히 기재")
        ));

        schema.put("rows", rows);
        metadata.put("schema", schema);
        return metadata;
    }
}