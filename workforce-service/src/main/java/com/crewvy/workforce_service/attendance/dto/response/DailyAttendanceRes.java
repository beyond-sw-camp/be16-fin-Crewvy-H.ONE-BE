package com.crewvy.workforce_service.attendance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class DailyAttendanceRes {

    private UUID memberId;
    private int sumWorkingDays;
    private Integer sumDaytimeOvertime;
    private Integer sumNightWork;
    private Integer sumHolidayWork;

}
