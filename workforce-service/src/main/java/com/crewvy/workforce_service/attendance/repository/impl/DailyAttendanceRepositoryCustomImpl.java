package com.crewvy.workforce_service.attendance.repository.impl;

import com.crewvy.workforce_service.attendance.entity.DailyAttendance;
import com.crewvy.workforce_service.attendance.repository.DailyAttendanceRepositoryCustom;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.crewvy.workforce_service.attendance.entity.QDailyAttendance.dailyAttendance;

@Repository
@RequiredArgsConstructor
public class DailyAttendanceRepositoryCustomImpl implements DailyAttendanceRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<DailyAttendance> findAllByDateRange(@Param("startDate") LocalDate startDate,
                                                    @Param("endDate") LocalDate endDate) {

        return queryFactory
                .selectFrom(dailyAttendance)
                .where(dailyAttendance.attendanceDate.between(startDate, endDate))
                .orderBy(dailyAttendance.memberId.asc(),
                        dailyAttendance.attendanceDate.asc())
                .fetch();
    }

    @Override
    public List<DailyAttendance> findAllByDateRangeAndCompany(
            @Param("companyId") UUID companyId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate) {

        return queryFactory
                .selectFrom(dailyAttendance)
                .where(dailyAttendance.companyId.eq(companyId)
                        .and(dailyAttendance.attendanceDate.between(startDate, endDate)))
                .orderBy(dailyAttendance.memberId.asc(),
                        dailyAttendance.attendanceDate.asc())
                .fetch();
    }


}
