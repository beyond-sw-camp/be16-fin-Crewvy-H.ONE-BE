package com.crewvy.workforce_service.approval.service;

import com.crewvy.common.entity.Bool;
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

            // --- 근태 관련 문서들 ---
            // --- 1. 휴가 신청서 ---
            Map<String, Object> leaveMetadata = createLeaveRequestMetadata(); // 메서드로 분리
            ApprovalDocument leaveDoc = ApprovalDocument.builder()
                    .documentName("휴가 신청서")
                    .metadata(leaveMetadata)
                    .isDirectCreatable(Bool.FALSE)
                    .build();
            documentRepository.save(leaveDoc);
            log.info("근태관련 문서 생성: {}", leaveDoc.getDocumentName());

            // --- 2. 출장 신청서 ---
            Map<String, Object> tripMetadata = createBusinessTripMetadata();
            ApprovalDocument tripDoc = ApprovalDocument.builder()
                    .documentName("출장 신청서")
                    .metadata(tripMetadata)
                    .isDirectCreatable(Bool.FALSE)
                    .build();
            documentRepository.save(tripDoc);
            log.info("근태관련 문서 생성: {}", tripDoc.getDocumentName());

            // --- 3. 휴직 신청서 ---
            Map<String, Object> leaveAbsenceMetadata = createLeaveOfAbsenceMetadata();
            ApprovalDocument leaveAbsenceDoc = ApprovalDocument.builder()
                    .documentName("휴직 신청서")
                    .metadata(leaveAbsenceMetadata)
                    .isDirectCreatable(Bool.FALSE)
                    .build();
            documentRepository.save(leaveAbsenceDoc);
            log.info("근태관련 문서 생성: {}", leaveAbsenceDoc.getDocumentName());

            // --- 4. 추가근무 신청서 ---
            Map<String, Object> overTimeMetadata = createOvertimeRequestTemplate();
            ApprovalDocument overTimeDoc = ApprovalDocument.builder()
                    .documentName("추가근무 신청서")
                    .metadata(overTimeMetadata)
                    .isDirectCreatable(Bool.FALSE)
                    .build();
            documentRepository.save(overTimeDoc);
            log.info("근태관련 문서 생성: {}", overTimeDoc.getDocumentName());

            log.info("=== 근태관련 결재 문서 생성 완료 ===");

            // --- (다른 기본 문서들 추가) ---

            // --- 5. 지출결의서 ---
            Map<String, Object> expenseReportMetadata = createExpenseReportTemplate();
            ApprovalDocument expenseReportDoc = ApprovalDocument.builder()
                    .documentName("지출결의서")
                    .metadata(expenseReportMetadata)
                    .isDirectCreatable(Bool.TRUE)
                    .build();
            documentRepository.save(expenseReportDoc);
            log.info("기본 문서 생성: {}", expenseReportDoc.getDocumentName());

            // --- 6. 구매품의서 ---
            Map<String, Object> purchaseRequestMetadata = createPurchaseRequestTemplate();
            ApprovalDocument purchaseRequestDoc = ApprovalDocument.builder()
                    .documentName("구매품의서")
                    .metadata(purchaseRequestMetadata)
                    .isDirectCreatable(Bool.TRUE)
                    .build();
            documentRepository.save(purchaseRequestDoc);
            log.info("기본 문서 생성: {}", purchaseRequestDoc.getDocumentName());

            // --- 7. 기안서(일반 품의) ---
            Map<String, Object> generalProposalMetadata = createGeneralProposalTemplate();
            ApprovalDocument generalProposalDoc = ApprovalDocument.builder()
                    .documentName("기안서(일반 품의)")
                    .metadata(generalProposalMetadata)
                    .isDirectCreatable(Bool.TRUE)
                    .build();
            documentRepository.save(generalProposalDoc);
            log.info("기본 문서 생성: {}", generalProposalDoc.getDocumentName());

            // --- 8. 업무 협조 요청서 ---
            Map<String, Object> workRequestMetadata = createWorkRequestTemplate();
            ApprovalDocument workRequestDoc = ApprovalDocument.builder()
                    .documentName("업무 협조 요청서")
                    .metadata(workRequestMetadata)
                    .isDirectCreatable(Bool.TRUE)
                    .build();
            documentRepository.save(workRequestDoc);
            log.info("기본 문서 생성: {}", workRequestDoc.getDocumentName());

            // --- 9. 증명서 발급 신청서 ---
            Map<String, Object> certificateRequestMetadata = createCertificateRequestTemplate();
            ApprovalDocument certificateRequestDoc = ApprovalDocument.builder()
                    .documentName("증명서 발급 신청서")
                    .metadata(certificateRequestMetadata)
                    .isDirectCreatable(Bool.TRUE)
                    .build();
            documentRepository.save(certificateRequestDoc);
            log.info("기본 문서 생성: {}", certificateRequestDoc.getDocumentName());

            // --- 10. 채용 품의서 ---
            Map<String, Object> recruitmentRequestMetadata = createRecruitmentRequestTemplate();
            ApprovalDocument recruitmentRequestDoc = ApprovalDocument.builder()
                    .documentName("채용 품의서")
                    .metadata(recruitmentRequestMetadata)
                    .isDirectCreatable(Bool.TRUE)
                    .build();
            documentRepository.save(recruitmentRequestDoc);
            log.info("기본 문서 생성: {}", recruitmentRequestDoc.getDocumentName());

            // --- 11. 계정 및 권한 신청서 ---
            Map<String, Object> accountRequestMetadata = createAccountRequestTemplate();
            ApprovalDocument accountRequestDoc = ApprovalDocument.builder()
                    .documentName("계정 및 권한 신청서")
                    .metadata(accountRequestMetadata)
                    .isDirectCreatable(Bool.TRUE)
                    .build();
            documentRepository.save(accountRequestDoc);
            log.info("기본 문서 생성: {}", accountRequestDoc.getDocumentName());

            log.info("=== 기본 결재 문서 생성 완료 ===");
        } else {
            log.info("=== 이미 결재 문서가 존재하여 건너<0xEB><0x9C><0x90>니다. ===");
        }
    }

    // --- Metadata 생성 헬퍼 메서드들 ---

    private Map<String, Object> createLeaveRequestMetadata() {

        // 1. 스키마(schema)는 동일하게 생성
        Map<String, Object> schema = new HashMap<>();
        List<List<Map<String, Object>>> rows = new ArrayList<>();

        // Row 1: 기본 정보
        rows.add(List.of(
                Map.of("id", "department", "label", "소속", "type", "text", "readonly", true),
                Map.of("id", "position", "label", "직급", "type", "text", "readonly", true),
                Map.of("id", "name", "label", "신청자", "type", "text", "readonly", true)
        ));
        // Row 2: 휴가 종류
        rows.add(List.of(
                Map.of("id", "requestType", "label", "휴가 종류", "type", "text", "required", true)
        ));
        // Row 3: 휴가 단위
        rows.add(List.of(
                Map.of("id", "requestUnit", "label", "휴가 단위", "type", "text", "required", true)
        ));
        // Row 4: 휴가 기간
        rows.add(List.of(
                Map.of("id", "startDate", "label", "시작일", "type", "datetime", "required", true),
                Map.of("id", "endDate", "label", "종료일", "type", "datetime", "required", true)
        ));
        // Row 5: 사유
        rows.add(List.of(
                Map.of("id", "reason", "label", "휴가 사유", "type", "textarea", "required", true,
                        "placeholder", "예: 가족 여행, 개인 사유")
        ));
        // Row 6: 비상 연락처
        rows.add(List.of(
                Map.of("id", "emergency_contact", "label", "비상 연락처", "type", "tel", "required", true,
                        "placeholder", "휴가 중 연락 가능한 번호")
        ));
        // Row 7: 업무 대리인
        rows.add(List.of(
                Map.of("id", "handover_agent", "label", "업무 대리인", "type", "text", "required", true,
                        "placeholder", "대리인의 이름과 소속을 기재")
        ));
        // Row 8: 인수인계 내용
        rows.add(List.of(
                Map.of("id", "handover_details", "label", "인수인계 내용", "type", "textarea", "required", true,
                        "placeholder", "부재 중 처리해야 할 업무 내용을 상세히 기재")
        ));

        schema.put("rows", rows);

        // 2. (*** 변경된 부분 ***)
        // 최상위 객체에 바로 모든 정보를 할당합니다.
        Map<String, Object> rootDocument = new HashMap<>();
        rootDocument.put("documentName", "휴가 신청서");
        rootDocument.put("description", "연차, 반차, 병가 등 개인 휴가를 신청하는 양식입니다.");
        rootDocument.put("schema", schema); // schema를 metadata 없이 바로 추가

        return rootDocument;
    }

    private Map<String, Object> createBusinessTripMetadata() {

        // 1. 스키마(schema)는 동일하게 생성
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
                Map.of("id", "workLocation", "label", "출장지", "type", "text", "required", true,
                        "placeholder", "예: 부산광역시 해운대구")
        ));
        // Row 3: 출장 기간
        rows.add(List.of(
                Map.of("id", "startDate", "label", "출장 시작일", "type", "datetime", "required", true),
                Map.of("id", "endDate", "label", "출장 종료일", "type", "datetime", "required", true)
        ));
        // Row 4: 출장 목적
        rows.add(List.of(
                Map.of("id", "reason", "label", "출장 목적", "type", "textarea", "required", true,
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

        // 2. (*** 변경된 부분 ***)
        // 최상위 객체에 바로 모든 정보를 할당합니다.
        Map<String, Object> rootDocument = new HashMap<>();
        rootDocument.put("documentName", "출장 신청서");
        rootDocument.put("description", "업무상 출장 시, 목적과 경비를 사전에 승인받기 위한 양식입니다.");
        rootDocument.put("schema", schema); // schema를 metadata 없이 바로 추가

        return rootDocument;
    }

    private Map<String, Object> createLeaveOfAbsenceMetadata() {

        // 1. 스키마(schema)는 동일하게 생성
        Map<String, Object> schema = new HashMap<>();
        List<List<Map<String, Object>>> rows = new ArrayList<>();

        // Row 1: 기본 정보
        rows.add(List.of(
                Map.of("id", "department", "label", "소속", "type", "text", "readonly", true),
                Map.of("id", "position", "label", "직급", "type", "text", "readonly", true),
                Map.of("id", "name", "label", "신청자", "type", "text", "readonly", true)
        ));

        // Row 2: 휴직 종류 (id: "requestType", type: "text")
        rows.add(List.of(
                Map.of("id", "requestType", "label", "휴직 종류", "type", "text", "required", true)
        ));

        // Row 3: 휴직 기간 (id: "startDate", "endDate")
        rows.add(List.of(
                Map.of("id", "startDate", "label", "휴직 시작일", "type", "date", "required", true),
                Map.of("id", "endDate", "label", "휴직 종료일", "type", "date", "required", true)
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

        // 2. (*** 변경된 부분 ***)
        // 최상위 객체에 바로 모든 정보를 할당합니다.
        Map<String, Object> rootDocument = new HashMap<>();
        rootDocument.put("documentName", "휴직 신청서");
        rootDocument.put("description", "질병, 육아, 자기계발 등 개인 사유로 인한 휴직을 신청하는 양식입니다.");
        rootDocument.put("schema", schema); // schema를 metadata 없이 바로 추가

        return rootDocument;
    }

    private Map<String, Object> createOvertimeRequestTemplate() {

        // 1. 스키마(schema) 생성
        Map<String, Object> schema = new HashMap<>();
        List<List<Map<String, Object>>> rows = new ArrayList<>();

        // Row 1: 기본 정보
        rows.add(List.of(
                Map.of("id", "department", "label", "소속", "type", "text", "readonly", true),
                Map.of("id", "position", "label", "직급", "type", "text", "readonly", true),
                Map.of("id", "name", "label", "신청자", "type", "text", "readonly", true)
        ));

        // Row 2: 근무 유형
        rows.add(List.of(
                Map.of("id", "requestType", "label", "근무 유형", "type", "text", "required", true)
        ));

        // Row 3: 근무 시간 (datetime)
        rows.add(List.of(
                Map.of("id", "startDate", "label", "시작 시간", "type", "datetime", "required", true, "placeholder", "예: 19:00"),
                Map.of("id", "endDate", "label", "종료 시간", "type", "datetime", "required", true, "placeholder", "예: 21:00")
        ));

        // Row 4: 사유
        rows.add(List.of(
                Map.of("id", "reason", "label", "추가근무 사유", "type", "textarea", "required", true,
                        "placeholder", "수행할 업무 내용을 상세히 기재")
        ));

        schema.put("rows", rows);

        // 2. 최상위 객체에 바로 모든 정보를 할당
        Map<String, Object> rootDocument = new HashMap<>();
        rootDocument.put("documentName", "추가근무 신청서");
        rootDocument.put("description", "연장, 야간, 휴일 근무 등 정규 시간 외 근무를 신청하는 양식입니다.");
        rootDocument.put("schema", schema); // schema를 metadata 없이 바로 추가

        return rootDocument;
    }

    private Map<String, Object> createExpenseReportTemplate() {

        // 1. "schema" 객체 생성
        Map<String, Object> schema = new HashMap<>();
        List<List<Map<String, Object>>> rows = new ArrayList<>();

        // Row 1: 기본 정보
        rows.add(List.of(
                Map.of("id", "department", "label", "소속", "type", "text", "readonly", true),
                Map.of("id", "position", "label", "직급", "type", "text", "readonly", true),
                Map.of("id", "name", "label", "신청자", "type", "text", "readonly", true)
        ));

        // Row 3: 총 청구 금액
        rows.add(List.of(
                Map.of("id", "totalAmount", "label", "총 청구 금액", "type", "number", "readonly", true, "placeholder", "하단 내역 합계 자동 계산")
        ));

        // Row 4: 지출 상세 내역 (Grid)
        // Grid의 columns 정의
        List<Map<String, Object>> gridColumns = new ArrayList<>();
        gridColumns.add(Map.of("id", "expenseDate", "label", "사용일", "type", "date", "required", true));
        gridColumns.add(Map.of(
                "id", "accountCategory",
                "label", "지출항목",
                "type", "select",
                "required", true,
                "options", List.of("식비", "교통비", "숙박비", "소모품비", "기타")
        ));
        gridColumns.add(Map.of("id", "description", "label", "내용", "type", "text", "required", true, "placeholder", "예: OOO 식당 (3명)"));
        gridColumns.add(Map.of("id", "amount", "label", "금액 (원)", "type", "number", "required", true));
        gridColumns.add(Map.of(
                "id", "proof",
                "label", "증빙",
                "type", "select",
                "options", List.of("법인카드", "개인카드(영수증)", "현금(간이영수증)")
        ));

        // Grid 자체를 Map으로 정의
        Map<String, Object> expenseGrid = new HashMap<>();
        expenseGrid.put("id", "expenseItems");
        expenseGrid.put("label", "지출 상세 내역");
        expenseGrid.put("type", "grid");
        expenseGrid.put("required", true);
        expenseGrid.put("columns", gridColumns);

        // Grid를 row에 추가
        rows.add(List.of(expenseGrid));

        // Row 5: 계좌 정보
        rows.add(List.of(
                Map.of("id", "bankName", "label", "은행명", "type", "text", "required", true, "placeholder", "예: OO은행"),
                Map.of("id", "accountNumber", "label", "계좌번호", "type", "text", "required", true, "placeholder", "'-' 없이 숫자만 입력"),
                Map.of("id", "accountHolder", "label", "예금주", "type", "text", "required", true, "placeholder", "신청자 본인 명의")
        ));

        // Row 6: 기타 사항
        rows.add(List.of(
                Map.of("id", "remarks", "label", "기타 사항", "type", "textarea", "required", false, "placeholder", "참고 사항 기재")
        ));

        // 2. schema에 rows 추가
        schema.put("rows", rows);

        // 3. 최상위(Root) 객체 생성 및 반환 (*** metadata 래퍼 없음 ***)
        Map<String, Object> rootDocument = new HashMap<>();
        rootDocument.put("documentName", "지출결의서");
        rootDocument.put("description", "경비 지출 내역을 정산 및 청구하기 위한 양식입니다.");
        rootDocument.put("schema", schema); // schema를 metadata 없이 바로 추가

        return rootDocument;
    }

    private Map<String, Object> createPurchaseRequestTemplate() {

        // 1. "schema" 객체 생성
        Map<String, Object> schema = new HashMap<>();
        List<List<Map<String, Object>>> rows = new ArrayList<>();

        // Row 1: 기본 정보
        rows.add(List.of(
                Map.of("id", "department", "label", "소속", "type", "text", "readonly", true),
                Map.of("id", "position", "label", "직급", "type", "text", "readonly", true),
                Map.of("id", "name", "label", "신청자", "type", "text", "readonly", true)
        ));

        // Row 3: 총 예상 금액
        rows.add(List.of(
                Map.of("id", "totalEstimatedAmount", "label", "총 예상 금액 (VAT 포함)", "type", "number", "readonly", true, "placeholder", "하단 내역 합계 자동 계산")
        ));

        // Row 4: 구매 품목 상세 내역 (Grid)
        // Grid의 columns 정의
        List<Map<String, Object>> gridColumns = new ArrayList<>();
        gridColumns.add(Map.of("id", "itemName", "label", "품목명", "type", "text", "required", true, "placeholder", "예: 32인치 4K 모니터"));
        gridColumns.add(Map.of("id", "specification", "label", "규격/모델", "type", "text", "required", false, "placeholder", "예: 델 U3223QE"));
        gridColumns.add(Map.of("id", "quantity", "label", "수량", "type", "number", "required", true));
        gridColumns.add(Map.of("id", "estimatedUnitPrice", "label", "예상 단가", "type", "number", "required", true));
        gridColumns.add(Map.of("id", "estimatedTotalPrice", "label", "예상 금액", "type", "number", "readonly", true, "placeholder", "수량 * 단가"));

        // Grid 자체를 Map으로 정의
        Map<String, Object> purchaseGrid = new HashMap<>();
        purchaseGrid.put("id", "purchaseItems");
        purchaseGrid.put("label", "구매 품목 상세 내역");
        purchaseGrid.put("type", "grid");
        purchaseGrid.put("required", true);
        purchaseGrid.put("columns", gridColumns);

        // Grid를 row에 추가
        rows.add(List.of(purchaseGrid));

        // Row 5: 구매 사유 (제공된 JSON 순서 기준)
        rows.add(List.of(
                Map.of("id", "reason", "label", "구매 사유", "type", "textarea", "required", true, "placeholder", "구매가 필요한 이유와 기대 효과를 상세히 기재")
        ));

        // Row 6: 거래처, 납기 요청일
        rows.add(List.of(
                Map.of("id", "vendor", "label", "거래처(구매처)", "type", "text", "required", false, "placeholder", "지정된 거래처가 있는 경우 기재"),
                Map.of("id", "deliveryDueDate", "label", "납기 요청일", "type", "date", "required", false)
        ));

        // Row 7: 기타 사항
        rows.add(List.of(
                Map.of("id", "remarks", "label", "기타 사항", "type", "textarea", "required", false, "placeholder", "참고 사항 (예: 견적서 첨부)")
        ));

        // 2. schema에 rows 추가
        schema.put("rows", rows);

        // 3. 최상위(Root) 객체 생성 및 반환
        Map<String, Object> rootDocument = new HashMap<>();
        rootDocument.put("documentName", "구매품의서");
        rootDocument.put("description", "업무에 필요한 물품 구매 또는 서비스 이용을 사전에 승인받기 위한 양식입니다.");
        rootDocument.put("schema", schema); // schema를 metadata 없이 바로 추가

        return rootDocument;
    }

    private Map<String, Object> createGeneralProposalTemplate() {

        // 1. "schema" 객체 생성
        Map<String, Object> schema = new HashMap<>();
        List<List<Map<String, Object>>> rows = new ArrayList<>();

        // Row 1: 기본 정보
        rows.add(List.of(
                Map.of("id", "department", "label", "소속", "type", "text", "readonly", true),
                Map.of("id", "position", "label", "직급", "type", "text", "readonly", true),
                Map.of("id", "name", "label", "신청자", "type", "text", "readonly", true)
        ));

        // Row 3: 시행 요청일
        rows.add(List.of(
                Map.of("id", "effectiveDate", "label", "시행 요청일", "type", "date", "required", false, "placeholder", "승인 후 시행을 희망하는 날짜")
        ));

        // Row 4: 요약
        rows.add(List.of(
                Map.of("id", "summary", "label", "요약", "type", "textarea", "required", true,
                        "placeholder", "본 기안의 목적과 핵심 내용을 3줄 이내로 요약하여 기재")
        ));

        // Row 5: 상세 내용
        rows.add(List.of(
                Map.of("id", "details", "label", "상세 내용", "type", "textarea", "required", true,
                        "placeholder", "1. 현황 및 문제점\n2. 제안 내용 (개선 방안)\n3. 세부 실행 계획")
        ));

        // Row 6: 기대 효과
        rows.add(List.of(
                Map.of("id", "expectedEffects", "label", "기대 효과", "type", "textarea", "required", true,
                        "placeholder", "본 기안이 승인/실행될 경우 예상되는 긍정적인 결과 (정량적, 정성적)")
        ));

        // Row 7: 관련 예산 (선택)
        rows.add(List.of(
                Map.of("id", "estimatedBudget", "label", "관련 예산 (선택)", "type", "number", "required", false, "placeholder", "예산이 소요되는 경우 기재 (단위: 원)")
        ));

        // Row 8: 기타 사항
        rows.add(List.of(
                Map.of("id", "remarks", "label", "기타 사항", "type", "textarea", "required", false, "placeholder", "참고 사항 (예: 관련 자료 첨부)")
        ));

        // 2. schema에 rows 추가
        schema.put("rows", rows);

        // 3. 최상위(Root) 객체 생성 및 반환
        Map<String, Object> rootDocument = new HashMap<>();
        rootDocument.put("documentName", "기안서 (일반 품의)");
        rootDocument.put("description", "특정 양식이 없는 일반적인 업무 제안, 보고, 승인 요청 시 사용하는 범용 양식입니다.");
        rootDocument.put("schema", schema); // schema를 metadata 없이 바로 추가

        return rootDocument;
    }

    private Map<String, Object> createWorkRequestTemplate() {

        // 1. "schema" 객체 생성
        Map<String, Object> schema = new HashMap<>();
        List<List<Map<String, Object>>> rows = new ArrayList<>();

        // Row 1: 기본 정보
        rows.add(List.of(
                Map.of("id", "department", "label", "요청 부서", "type", "text", "readonly", true),
                Map.of("id", "name", "label", "요청자", "type", "text", "readonly", true)
        ));

        // Row 3: 협조 부서/담당자
        rows.add(List.of(
                Map.of("id", "recipientDept", "label", "협조 부서", "type", "text", "required", true, "placeholder", "예: 디자인팀"),
                Map.of("id", "recipientName", "label", "협조 담당자", "type", "text", "required", false, "placeholder", "(지정 담당자가 있는 경우 기재)")
        ));

        // Row 4: 완료 요청일, 긴급도
        rows.add(List.of(
                Map.of("id", "dueDate", "label", "완료 요청일", "type", "date", "required", true),
                Map.of(
                        "id", "priority",
                        "label", "긴급도",
                        "type", "select",
                        "required", true,
                        "options", List.of("긴급", "높음", "보통")
                )
        ));

        // Row 5: 요청 내용
        rows.add(List.of(
                Map.of("id", "requestDetails", "label", "요청 내용", "type", "textarea", "required", true,
                        "placeholder", "1. 요청 배경\n2. 주요 요청 사항 (상세히)\n3. 참고 자료")
        ));

        // Row 6: 기타 사항
        rows.add(List.of(
                Map.of("id", "remarks", "label", "기타 사항", "type", "textarea", "required", false, "placeholder", "참고 사항 (예: 관련 기획안 별도 첨부)")
        ));

        // 2. schema에 rows 추가
        schema.put("rows", rows);

        // 3. 최상위(Root) 객체 생성 및 반환
        Map<String, Object> rootDocument = new HashMap<>();
        rootDocument.put("documentName", "업무 협조 요청서");
        rootDocument.put("description", "타 부서 또는 담당자에게 공식적으로 업무 지원을 요청하는 양식입니다.");
        rootDocument.put("schema", schema); // schema를 metadata 없이 바로 추가

        return rootDocument;
    }

    private Map<String, Object> createCertificateRequestTemplate() {

        // 1. "schema" 객체 생성
        Map<String, Object> schema = new HashMap<>();
        List<List<Map<String, Object>>> rows = new ArrayList<>();

        // Row 1: 기본 정보
        rows.add(List.of(
                Map.of("id", "department", "label", "소속", "type", "text", "readonly", true),
                Map.of("id", "position", "label", "직급", "type", "text", "readonly", true),
                Map.of("id", "name", "label", "신청자", "type", "text", "readonly", true)
        ));

        // Row 2: 증명서 종류, 수량
        rows.add(List.of(
                Map.of(
                        "id", "certificateType",
                        "label", "증명서 종류",
                        "type", "select",
                        "required", true,
                        "options", List.of("재직증명서", "경력증명서", "퇴직증명서", "원천징수영수증", "기타")
                ),
                Map.of("id", "quantity", "label", "신청 수량", "type", "number", "required", true, "placeholder", "기본 1부")
        ));

        // Row 3: 제출 용도, 제출처
        rows.add(List.of(
                Map.of("id", "usage", "label", "제출 용도", "type", "text", "required", true, "placeholder", "예: 은행 제출용"),
                Map.of("id", "submissionTo", "label", "제출처", "type", "text", "required", false, "placeholder", "예: OO은행 OOO지점")
        ));

        // Row 4: 수령 방법
        rows.add(List.of(
                Map.of(
                        "id", "pickupMethod",
                        "label", "수령 방법",
                        "type", "select",
                        "required", true,
                        "options", List.of("사내 수령(방문)", "이메일(PDF)", "우편 수령")
                )
        ));

        // Row 5: 이메일 주소 (Conditional)
        rows.add(List.of(
                Map.of(
                        "id", "emailAddress",
                        "label", "이메일 주소",
                        "type", "email",
                        "required", false,
                        "placeholder", "'이메일 수령' 선택 시 기재",
                        "showIf", Map.of("field", "pickupMethod", "value", "이메일(PDF)")
                )
        ));

        // Row 6: 주소 (Conditional)
        rows.add(List.of(
                Map.of(
                        "id", "fullAddress",
                        "label", "주소",
                        "type", "text",
                        "required", false,
                        "placeholder", "'우편 수령' 선택 시 기재 (우편번호 포함)",
                        "showIf", Map.of("field", "pickupMethod", "value", "우편 수령")
                )
        ));

        // Row 7: 기타 요청사항
        rows.add(List.of(
                Map.of("id", "remarks", "label", "기타 요청사항", "type", "textarea", "required", false, "placeholder", "예: '기타' 선택 시 상세 내용 기재")
        ));

        // 2. schema에 rows 추가
        schema.put("rows", rows);

        // 3. 최상위(Root) 객체 생성 및 반환
        Map<String, Object> rootDocument = new HashMap<>();
        rootDocument.put("documentName", "증명서 발급 신청서");
        rootDocument.put("description", "재직/경력증명서 등 각종 증명서 발급을 신청하는 양식입니다.");
        rootDocument.put("schema", schema); // schema를 metadata 없이 바로 추가

        return rootDocument;
    }

    private Map<String, Object> createRecruitmentRequestTemplate() {

        // 1. "schema" 객체 생성
        Map<String, Object> schema = new HashMap<>();
        List<List<Map<String, Object>>> rows = new ArrayList<>();

        // Row 1: 기본 정보
        rows.add(List.of(
                Map.of("id", "department", "label", "요청 부서", "type", "text", "readonly", true),
                Map.of("id", "position", "label", "직급", "type", "text", "readonly", true),
                Map.of("id", "name", "label", "기안자", "type", "text", "readonly", true)
        ));

        // Row 2: 채용 포지션, 형태
        rows.add(List.of(
                Map.of("id", "positionTitle", "label", "채용 포지션", "type", "text", "required", true, "placeholder", "예: 백엔드 개발자 (경력 3년 이상)"),
                Map.of(
                        "id", "employmentType",
                        "label", "채용 형태",
                        "type", "select",
                        "required", true,
                        "options", List.of("정규직", "계약직", "인턴", "기타")
                )
        ));

        // Row 3: 인원, 필요 시점
        rows.add(List.of(
                Map.of("id", "headcount", "label", "채용 인원 (명)", "type", "number", "required", true),
                Map.of("id", "requiredDate", "label", "충원 필요 시점", "type", "date", "required", true, "placeholder", "예: 즉시")
        ));

        // Row 4: 충원 사유
        rows.add(List.of(
                Map.of("id", "reason", "label", "충원 사유", "type", "textarea", "required", true,
                        "placeholder", "1. 결원 발생 (퇴사자 OOO 대체)\n2. 신규 사업 확장 (OOO 프로젝트)")
        ));

        // Row 5: 주요 업무 (R&R)
        rows.add(List.of(
                Map.of("id", "responsibilities", "label", "주요 업무 (R&R)", "type", "textarea", "required", true,
                        "placeholder", "채용될 인력이 수행할 주요 업무 리스트")
        ));

        // Row 6: 자격 요건
        rows.add(List.of(
                Map.of("id", "qualifications", "label", "자격 요건", "type", "textarea", "required", true,
                        "placeholder", "필수 스킬, 우대 사항, 필요 경력 등")
        ));

        // Row 7: 기타 사항
        rows.add(List.of(
                Map.of("id", "remarks", "label", "기타 사항", "type", "textarea", "required", false,
                        "placeholder", "예상 연봉 범위, 면접관 지정 등")
        ));

        // 2. schema에 rows 추가
        schema.put("rows", rows);

        // 3. 최상위(Root) 객체 생성 및 반환
        Map<String, Object> rootDocument = new HashMap<>();
        rootDocument.put("documentName", "채용 품의서");
        rootDocument.put("description", "결원 보충 또는 신규 증원이 필요할 시, 채용을 승인받기 위한 양식입니다.");
        rootDocument.put("schema", schema); // schema를 metadata 없이 바로 추가

        return rootDocument;
    }

    private Map<String, Object> createAccountRequestTemplate() {

        // 1. "schema" 객체 생성
        Map<String, Object> schema = new HashMap<>();
        List<List<Map<String, Object>>> rows = new ArrayList<>();

        // Row 1: 기본 정보
        rows.add(List.of(
                Map.of("id", "department", "label", "요청 부서", "type", "text", "readonly", true),
                Map.of("id", "name", "label", "요청자", "type", "text", "readonly", true)
        ));

        // Row 2: 대상자
        rows.add(List.of(
                Map.of("id", "targetUser", "label", "대상자", "type", "text", "required", true, "placeholder", "예: OOO (신규 입사자), 본인")
        ));

        // Row 3: 요청 구분, 대상 시스템
        rows.add(List.of(
                Map.of(
                        "id", "requestType",
                        "label", "요청 구분",
                        "type", "select",
                        "required", true,
                        "options", List.of("신규 계정 생성", "권한 변경", "권한 삭제", "비밀번호 초기화")
                ),
                Map.of("id", "targetSystem", "label", "대상 시스템", "type", "text", "required", true, "placeholder", "예: 그룹웨어, ERP, 파일서버")
        ));

        // Row 4: 요청 내용
        rows.add(List.of(
                Map.of("id", "requestDetails", "label", "요청 내용", "type", "textarea", "required", true,
                        "placeholder", "'신규'의 경우: 기본 권한 요청\n'변경'의 경우: (AS-IS) / (TO-BE) 상세히 기재")
        ));

        // Row 5: 요청 사유
        rows.add(List.of(
                Map.of("id", "reason", "label", "요청 사유", "type", "textarea", "required", true, "placeholder", "예: 신규 입사, 부서 이동, 업무 변경")
        ));

        // Row 6: 처리 희망일
        rows.add(List.of(
                Map.of("id", "dueDate", "label", "처리 희망일", "type", "date", "required", false)
        ));

        // 2. schema에 rows 추가
        schema.put("rows", rows);

        // 3. 최상위(Root) 객체 생성 및 반환
        Map<String, Object> rootDocument = new HashMap<>();
        rootDocument.put("documentName", "계정 및 권한 신청서");
        rootDocument.put("description", "사내 시스템의 계정 생성, 권한 변경, 삭제를 IT팀에 요청하는 양식입니다.");
        rootDocument.put("schema", schema); // schema를 metadata 없이 바로 추가

        return rootDocument;
    }
}