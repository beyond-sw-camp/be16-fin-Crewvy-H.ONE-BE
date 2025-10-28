package com.crewvy.workforce_service.salary.service;

import com.crewvy.workforce_service.salary.constant.HolidayRule;
import com.crewvy.workforce_service.salary.repository.HolidayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class HolidayService {

    private final HolidayRepository holidayRepository;

    public boolean isWeekendOrHoliday(LocalDate date) {
        if (date == null) {
            return false;
        }

        // 주말 확인 Java 내장 기능 사용
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return true; // 주말이면 바로 true 반환
        }

        // 주말이 아니면 db 에서 공휴일인지 확인
        return holidayRepository.existsBySolarDate(date);
    }

    public LocalDate adjustForHoliday(LocalDate date, HolidayRule holidayRule) {
        if (date == null || holidayRule == null) {
            return date;
        }

        LocalDate adjustedDate = date;
        // isWeekendOrHoliday를 사용하여 주말 또는 공휴일이 아닐 때까지 날짜 조정
        while (isWeekendOrHoliday(adjustedDate)) {
            switch (holidayRule) {
                case PREPAID:
                    adjustedDate = adjustedDate.minusDays(1);
                    break;
                case POSTPAID:
                    adjustedDate = adjustedDate.plusDays(1);
                    break;
                default:
                    return date;
            }
        }
        return adjustedDate;
    }
}
