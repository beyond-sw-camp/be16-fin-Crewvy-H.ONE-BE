package com.crewvy.workforce_service.attendance.dto.response;

import com.crewvy.workforce_service.attendance.constant.EventType;
import com.crewvy.workforce_service.attendance.entity.DailyAttendance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodayAttendanceStatusResponse {

    private DailyAttendance dailyAttendance;
    private EventType lastEventType;
    private Double previousMonthAttendanceRate; // 전월 근속비율 (%)

    public static TodayAttendanceStatusResponse from(DailyAttendance dailyAttendance, EventType lastEventType) {
        return TodayAttendanceStatusResponse.builder()
                .dailyAttendance(dailyAttendance)
                .lastEventType(lastEventType)
                .build();
    }

    public static TodayAttendanceStatusResponse from(DailyAttendance dailyAttendance, EventType lastEventType, Double previousMonthAttendanceRate) {
        return TodayAttendanceStatusResponse.builder()
                .dailyAttendance(dailyAttendance)
                .lastEventType(lastEventType)
                .previousMonthAttendanceRate(previousMonthAttendanceRate)
                .build();
    }
}
