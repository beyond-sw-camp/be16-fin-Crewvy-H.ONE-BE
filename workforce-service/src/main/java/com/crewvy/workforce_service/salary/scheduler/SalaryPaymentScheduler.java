package com.crewvy.workforce_service.salary.scheduler;

import com.crewvy.workforce_service.salary.entity.Salary;
import com.crewvy.workforce_service.salary.constant.SalaryStatus;
import com.crewvy.workforce_service.salary.repository.SalaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SalaryPaymentScheduler {

    private final SalaryRepository salaryRepository;

    @Scheduled(cron = "0 0 10 * * ?")
    @Transactional
    @SchedulerLock(
            name = "simulatePaymentCompletion", // ★ 중요: 작업별로 고유한 이름 지정
            lockAtMostFor = "PT10M",  // 작업이 10분 이상 걸리면 강제 잠금 해제
            lockAtLeastFor = "PT30S"  // 작업이 빨리 끝나도 최소 30초간 잠금 유지
    )
    public void simulatePaymentCompletion() {
        LocalDate today = LocalDate.now();

        List<Salary> targets = salaryRepository.findAllBySalaryStatusAndPaymentDateBefore(
                SalaryStatus.PENDING,
                today
        );

        if (targets.isEmpty()) {
            log.info("[Scheduler] 지급 완료로 처리할 급여 데이터가 없습니다.");
            return;
        }

        for (Salary salary : targets) {
            salary.updateSalaryStatus(SalaryStatus.PAID);
        }
        salaryRepository.saveAll(targets);

        log.info("[Scheduler] 총 {}건의 급여 데이터를 'PAID' 상태로 변경했습니다.", targets.size());
    }
}
