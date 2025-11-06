package com.crewvy.workforce_service.attendance.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * ShedLock 분산 락 설정
 * MSA 환경에서 여러 인스턴스가 동시에 배치 작업을 실행하는 것을 방지
 *
 * 동작 원리:
 * 1. @Scheduled 메서드에 @SchedulerLock 어노테이션 추가
 * 2. 첫 번째 인스턴스가 DB에 락 획득
 * 3. 다른 인스턴스들은 락이 있으면 실행 스킵
 * 4. 작업 완료 시 자동으로 락 해제
 *
 * 예시:
 * <pre>
 * {@code
 * @Scheduled(cron = "0 0 3 1 * *")
 * @SchedulerLock(
 *     name = "annual_leave_accrual_lock",
 *     lockAtMostFor = "30m",  // 최대 30분 (서버 다운 대비)
 *     lockAtLeastFor = "5m"   // 최소 5분 (과도한 재실행 방지)
 * )
 * public void runBatch() { ... }
 * }
 * </pre>
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M") // 기본값: 10분
public class ShedlockConfig {

    /**
     * JDBC 기반 LockProvider 생성
     * shedlock 테이블에 락 정보 저장
     *
     * @param dataSource JPA DataSource 재사용
     * @return LockProvider
     */
    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(new JdbcTemplate(dataSource))
                .usingDbTime() // DB 서버 시간 사용 (인스턴스 간 시간 불일치 방지)
                .build()
        );
    }
}
