package com.crewvy.workforce_service.reservation.scheduler;

import com.crewvy.common.dto.NotificationMessage;
import com.crewvy.workforce_service.reservation.entity.Reservation;
import com.crewvy.workforce_service.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationScheduler {

    private final ApplicationEventPublisher eventPublisher;
    private final ReservationRepository reservationRepository;

    @Transactional
    @Scheduled(cron = "0 0 1 * * *")
    @SchedulerLock(
            name = "notificationForReservation", // ★ 중요: 작업별로 고유한 이름 지정
            lockAtMostFor = "PT10M",  // 작업이 10분 이상 걸리면 강제 잠금 해제
            lockAtLeastFor = "PT30S"  // 작업이 빨리 끝나도 최소 30초간 잠금 유지
    )
    public void notificationForReservation() {
        // 예약 당일
        LocalDate today = LocalDate.now();
        // 예약 하루 전 (현재 + 1일)
        LocalDate oneDayAfter = today.plusDays(1);

        // 예약 현재 날짜를 기준으로 당일, 하루 후 예약 조회
        List<Reservation> reservationList = reservationRepository.findAllByStartDateTimeIn(
                List.of(today, oneDayAfter)
        );

        for (Reservation reservation : reservationList) {

            // 예약 등록 직원
            UUID receiverId = reservation.getMemberId();

            // 메세지 조합
            NotificationMessage message = NotificationMessage.builder()
                    .memberId(receiverId)
                    .notificationType("NT010")
                    .content(reservation.getTitle() + " 예약이 예정되어 있습니다.")
                    .targetId(reservation.getId())
                    .build();

            eventPublisher.publishEvent(message);
        }
    }
}
