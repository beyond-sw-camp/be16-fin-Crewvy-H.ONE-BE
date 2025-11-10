package com.crewvy.workforce_service.attendance.dto.response;

import com.crewvy.workforce_service.salary.entity.Holidays;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 공휴일 정보 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HolidayRes {
    private LocalDate date;      // 양력 날짜
    private String name;          // 공휴일 이름

    public static HolidayRes from(Holidays holiday) {
        return HolidayRes.builder()
                .date(holiday.getSolarDate())
                .name(holiday.getMemo())
                .build();
    }
}
