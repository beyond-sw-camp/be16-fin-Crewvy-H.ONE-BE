package com.crewvy.workforce_service.attendance.kafka;

import com.crewvy.workforce_service.attendance.service.AnnualLeaveAccrualService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Member-Service의 이벤트를 수신하는 Kafka Listener
 * - member-create: 신규 직원 생성 이벤트 처리 (초기 연차 부여)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberEventListener {

    private final AnnualLeaveAccrualService annualLeaveAccrualService;

    /**
     * 신규 직원 생성 이벤트 처리
     * Member-Service에서 직원 생성 시 memberId를 전송받아 초기 연차 부여
     *
     * @param payload memberId (String 형식)
     */
    @KafkaListener(
            topics = "member-create",
            containerFactory = "outboxEventKafkaListenerFactory"
    )
    public void handleMemberCreated(String payload) {
        try {
            log.info("========================================");
            log.info("신규 직원 생성 이벤트 수신: payload={}", payload);

            // 1. String payload를 UUID로 변환
            UUID memberId = UUID.fromString(payload);
            log.info("변환된 memberId: {}", memberId);

            // 2. 초기 연차 부여 로직 호출
            annualLeaveAccrualService.grantInitialAnnualLeave(memberId);

            log.info("신규 직원 초기 연차 부여 완료: memberId={}", memberId);
            log.info("========================================");

        } catch (IllegalArgumentException e) {
            log.error("잘못된 payload 형식: payload={}", payload, e);
        } catch (Exception e) {
            log.error("신규 직원 이벤트 처리 실패: payload={}", payload, e);
            // TODO: 재시도 로직 또는 Dead Letter Queue 전송 고려
        }
    }
}
