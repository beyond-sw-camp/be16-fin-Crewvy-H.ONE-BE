package com.crewvy.workforce_service.attendance.event;

import com.crewvy.workforce_service.approval.constant.ApprovalState;
import com.crewvy.workforce_service.attendance.constant.AttendanceStatus;
import com.crewvy.workforce_service.attendance.constant.PolicyTypeCode;
import com.crewvy.workforce_service.attendance.constant.RequestStatus;
import com.crewvy.workforce_service.attendance.constant.RequestUnit;
import com.crewvy.workforce_service.attendance.entity.DailyAttendance;
import com.crewvy.workforce_service.attendance.entity.MemberBalance;
import com.crewvy.workforce_service.attendance.entity.Request;
import com.crewvy.workforce_service.attendance.repository.DailyAttendanceRepository;
import com.crewvy.workforce_service.attendance.repository.MemberBalanceRepository;
import com.crewvy.workforce_service.attendance.repository.RequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 근태 Request 승인/반려 이벤트 처리
 * ApprovalService에서 결재 완료 시 발행한 이벤트를 수신하여
 * Request 상태 업데이트 및 DailyAttendance 생성/수정 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceRequestEventListener {

    private final RequestRepository requestRepository;
    private final DailyAttendanceRepository dailyAttendanceRepository;
    private final MemberBalanceRepository memberBalanceRepository;

    /**
     * 결재 승인/반려 완료 후 처리
     * - AFTER_COMMIT: 결재 트랜잭션이 커밋된 후 실행 (롤백 방지)
     * - REQUIRES_NEW: 새로운 트랜잭션에서 실행 (DB 변경사항 커밋을 위해 필수)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAttendanceRequestApproved(AttendanceRequestApprovedEvent event) {
        log.error("🎯🎯🎯 [이벤트 리스너] 근태 Request 결재 완료 이벤트 수신: requestId={}, approvalState={} 🎯🎯🎯",
                event.getRequestId(), event.getApprovalState());

        // 1. Request 조회
        Request request = requestRepository.findById(event.getRequestId())
                .orElseThrow(() -> new IllegalArgumentException("Request를 찾을 수 없습니다: " + event.getRequestId()));

        // 2. Request 상태 업데이트
        if (event.getApprovalState() == ApprovalState.APPROVED) {
            request.updateStatus(RequestStatus.APPROVED);
            requestRepository.save(request); // DB 반영
            log.error("✅✅✅ [이벤트 리스너] Request 승인 처리 완료 및 DB 저장: requestId={} ✅✅✅", request.getId());

            // 3. DailyAttendance 생성 또는 업데이트
            updateDailyAttendanceForApprovedRequest(request);

        } else if (event.getApprovalState() == ApprovalState.REJECTED) {
            request.updateStatus(RequestStatus.REJECTED);

            // 거절 시 잔액 복구 (신청 시 차감했던 잔액 되돌림)
            restoreBalanceAfterRejection(request);

            requestRepository.save(request); // DB 반영
            log.error("❌❌❌ [이벤트 리스너] Request 반려 처리 및 잔액 복구 완료: requestId={} ❌❌❌", request.getId());

        } else if (event.getApprovalState() == ApprovalState.DISCARDED) {
            request.updateStatus(RequestStatus.CANCELED);

            // 취소 시 잔액 복구 (신청 시 차감했던 잔액 되돌림)
            restoreBalanceAfterRejection(request);

            requestRepository.save(request); // DB 반영
            log.error("🗑️🗑️🗑️ [이벤트 리스너] Request 취소 처리 및 잔액 복구 완료: requestId={} 🗑️🗑️🗑️", request.getId());
        }
    }

    /**
     * 승인된 Request에 대해 DailyAttendance를 생성하거나 상태를 업데이트
     */
    private void updateDailyAttendanceForApprovedRequest(Request request) {
        PolicyTypeCode policyTypeCode = request.getPolicy().getPolicyTypeCode();
        LocalDate startDate = request.getStartDateTime().toLocalDate();
        LocalDate endDate = request.getEndDateTime().toLocalDate();

        // 연차/휴가류, 출장만 DailyAttendance 상태 변경
        if (!isAttendanceRelatedPolicyType(policyTypeCode)) {
            log.debug("근태 기록과 무관한 정책 유형: {}", policyTypeCode);
            return;
        }

        // Request 기간 동안의 모든 날짜에 대해 DailyAttendance 생성/업데이트
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            createOrUpdateDailyAttendance(request, date);
        }
    }

    /**
     * 특정 날짜의 DailyAttendance 생성 또는 업데이트
     */
    private void createOrUpdateDailyAttendance(Request request, LocalDate date) {
        Optional<DailyAttendance> existingAttendance = dailyAttendanceRepository
                .findByMemberIdAndAttendanceDate(request.getMemberId(), date);

        AttendanceStatus status = mapPolicyTypeToAttendanceStatus(
                request.getPolicy().getPolicyTypeCode(),
                request.getRequestUnit()
        );

        if (existingAttendance.isPresent()) {
            // 기존 DailyAttendance가 있으면 상태만 업데이트
            DailyAttendance attendance = existingAttendance.get();
            attendance.updateStatus(status);
            log.info("DailyAttendance 상태 업데이트: memberId={}, date={}, status={}",
                    request.getMemberId(), date, status);

        } else {
            // 없으면 새로 생성
            DailyAttendance newAttendance = DailyAttendance.builder()
                    .memberId(request.getMemberId())
                    .companyId(request.getPolicy().getCompanyId())
                    .attendanceDate(date)
                    .status(status)
                    .firstClockIn(null)  // 휴가/출장은 출퇴근 없음
                    .lastClockOut(null)
                    .workedMinutes(0)
                    .build();

            dailyAttendanceRepository.save(newAttendance);
            log.info("DailyAttendance 생성: memberId={}, date={}, status={}",
                    request.getMemberId(), date, status);
        }
    }

    /**
     * PolicyTypeCode를 AttendanceStatus로 매핑
     */
    private AttendanceStatus mapPolicyTypeToAttendanceStatus(PolicyTypeCode policyTypeCode, RequestUnit requestUnit) {
        // RequestUnit에 따라 반차 구분
        if (policyTypeCode == PolicyTypeCode.ANNUAL_LEAVE) {
            if (requestUnit == RequestUnit.HALF_DAY_AM) {
                return AttendanceStatus.HALF_DAY_AM;
            } else if (requestUnit == RequestUnit.HALF_DAY_PM) {
                return AttendanceStatus.HALF_DAY_PM;
            } else {
                return AttendanceStatus.ANNUAL_LEAVE;
            }
        }

        // 다른 PolicyTypeCode는 직접 매핑
        return switch (policyTypeCode) {
            case MATERNITY_LEAVE -> AttendanceStatus.MATERNITY_LEAVE;
            case PATERNITY_LEAVE -> AttendanceStatus.PATERNITY_LEAVE;
            case CHILDCARE_LEAVE -> AttendanceStatus.CHILDCARE_LEAVE;
            case FAMILY_CARE_LEAVE -> AttendanceStatus.FAMILY_CARE_LEAVE;
            case MENSTRUAL_LEAVE -> AttendanceStatus.MENSTRUAL_LEAVE;
            case BUSINESS_TRIP -> AttendanceStatus.BUSINESS_TRIP;
            default -> AttendanceStatus.NORMAL_WORK;
        };
    }

    /**
     * DailyAttendance 기록이 필요한 정책 유형인지 확인
     */
    private boolean isAttendanceRelatedPolicyType(PolicyTypeCode policyTypeCode) {
        return switch (policyTypeCode) {
            case ANNUAL_LEAVE, MATERNITY_LEAVE, PATERNITY_LEAVE,
                 CHILDCARE_LEAVE, FAMILY_CARE_LEAVE, MENSTRUAL_LEAVE,
                 BUSINESS_TRIP -> true;
            default -> false;
        };
    }

    /**
     * 신청 거절 시 잔액 복구 (신청 시 차감했던 잔액 되돌림)
     */
    private void restoreBalanceAfterRejection(Request request) {
        if (request.getPolicy() == null || !request.getPolicy().getPolicyTypeCode().isBalanceDeductible()) {
            return; // 차감 불필요한 정책은 스킵
        }

        int currentYear = LocalDate.now().getYear();
        PolicyTypeCode typeCode = request.getPolicy().getPolicyTypeCode();

        MemberBalance balance = memberBalanceRepository
                .findByMemberIdAndBalanceTypeCodeAndYear(request.getMemberId(), typeCode, currentYear)
                .orElse(null);

        if (balance == null) {
            log.warn("잔액 복구 실패: 잔액 정보를 찾을 수 없음. memberId={}, typeCode={}, year={}",
                    request.getMemberId(), typeCode, currentYear);
            return;
        }

        // 잔액 복구 계산
        double newUsed = Math.max(0, balance.getTotalUsed() - request.getDeductionDays());
        double newRemaining = Math.min(balance.getTotalGranted(), balance.getRemaining() + request.getDeductionDays());

        // MemberBalance 업데이트
        MemberBalance updatedBalance = MemberBalance.builder()
                .id(balance.getId())
                .memberId(balance.getMemberId())
                .companyId(balance.getCompanyId())
                .balanceTypeCode(balance.getBalanceTypeCode())
                .year(balance.getYear())
                .totalGranted(balance.getTotalGranted())
                .totalUsed(newUsed)
                .remaining(newRemaining)
                .expirationDate(balance.getExpirationDate())
                .isPaid(balance.getIsPaid())
                .isUsable(balance.getIsUsable())
                .build();

        memberBalanceRepository.save(updatedBalance);
        log.info("잔액 복구 완료: memberId={}, typeCode={}, 복구일수={}일, 복구 후 잔여={}/{}일",
                request.getMemberId(), typeCode, request.getDeductionDays(), newRemaining, balance.getTotalGranted());
    }
}
