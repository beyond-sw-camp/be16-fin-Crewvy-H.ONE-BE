package com.crewvy.workspace_service.calendar.service;

import com.crewvy.common.dto.ScheduleDeleteDto;
import com.crewvy.common.dto.ScheduleDto;
import com.crewvy.common.entity.Bool;
import com.crewvy.workspace_service.calendar.constant.CalendarType;
import com.crewvy.workspace_service.calendar.dto.request.PersonalScheduleReqDto;
import com.crewvy.workspace_service.calendar.dto.response.CalendarResDto;
import com.crewvy.workspace_service.calendar.entity.Calendar;
import com.crewvy.workspace_service.calendar.repository.CalendarRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CalendarService {
    private final CalendarRepository calendarRepository;

//    내 일정 전체조회
    @Transactional(readOnly = true)
    public List<CalendarResDto> findMySchedule(UUID memberId, String searchType, Integer year, Integer month) {
        List<Calendar> calendarList = null;

        if(searchType.equals("Day")) {
            calendarList = getTodayCalendar(memberId);
        }
        else if(searchType.equals("Week")) {
            calendarList = getThisWeekCalendar(memberId);
        }
        else if(searchType.equals("Month")) {
            calendarList = getMonthlyCalendar(memberId, year, month);
        }

        if (calendarList == null) {
            return Collections.emptyList(); // 또는 new ArrayList<>()
        }

        return calendarList.stream().map(CalendarResDto::from).toList();
    }

//    오늘 일정 조회
    private List<Calendar> getTodayCalendar(UUID memberId) {
        LocalDate today = LocalDate.now();
        LocalDateTime periodStart = today.atStartOfDay();        // 오늘 00:00:00
        LocalDateTime periodEnd = today.atTime(LocalTime.MAX); // 오늘 23:59:59...

        // (startDate <= 오늘 끝) AND (endDate >= 오늘 시작)
        return calendarRepository.findOverlappingEvents(
                memberId,
                Bool.FALSE,
                periodStart,
                periodEnd
        );
    }

//    이번주 일정 조회
    private List<Calendar> getThisWeekCalendar(UUID memberId) {
        LocalDate today = LocalDate.now();

        // 이번 주 월요일 00:00:00
        LocalDateTime periodStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();
        // 이번 주 일요일 23:59:59...
        LocalDateTime periodEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).atTime(LocalTime.MAX);

        // (startDate <= 일요일 끝) AND (endDate >= 월요일 시작)
        return calendarRepository.findOverlappingEvents(
                memberId,
                Bool.FALSE,
                periodStart,
                periodEnd
        );
    }

//    월별 일정 조회
    private List<Calendar> getMonthlyCalendar(UUID memberId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);

        // 이번 달 1일 00:00:00
        LocalDateTime periodStart = yearMonth.atDay(1).atStartOfDay();
        // 이번 달 마지막 날 23:59:59...
        LocalDateTime periodEnd = yearMonth.atEndOfMonth().atTime(LocalTime.MAX);

        // 리포지토리 호출은 동일
        return calendarRepository.findOverlappingEvents(
                memberId,
                Bool.FALSE,
                periodStart,
                periodEnd
        );
    }

//    내 개인일정 등록
    public UUID postMySchedule(UUID memberId, PersonalScheduleReqDto scheduleDto) {
        Calendar schedule = Calendar.builder()
                .memberId(memberId)
                .title(scheduleDto.getTitle())
                .contents(scheduleDto.getContents())
                .startDate(scheduleDto.getStartDate())
                .endDate(scheduleDto.getEndDate())
                .type(CalendarType.PERSONAL)
                .isDeleted(Bool.FALSE)
                .build();

        calendarRepository.save(schedule);

        return schedule.getId();
    }

//    개인일정 수정
    public void updateMySchedule(UUID id, PersonalScheduleReqDto dto) {
        Calendar calendar = calendarRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 일정입니다."));
        calendar.updateSchedule(dto.getTitle(), dto.getContents(), dto.getStartDate(), dto.getEndDate());
    }

//    개인일정 삭제
    public void deleteMySchedule(UUID id) {
        Calendar calendar = calendarRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 일정입니다."));
        calendar.deleteSchedule();
    }

//    개인 일정 외 일정 저장 및 수정
    public void saveSchedule(ScheduleDto dto) {
        Optional<Calendar> optionalCalendar = calendarRepository.findByOriginIdAndMemberIdAndIsDeleted(dto.getOriginId(), dto.getMemberId(), Bool.FALSE);

        if (optionalCalendar.isPresent()) {
            // [수정 로직] 이미 존재하면, 기존 일정을 업데이트

            Calendar calendar = optionalCalendar.get();
            calendar.updateSchedule(dto.getTitle(), dto.getContents(), dto.getStartDate(), dto.getEndDate());

            calendarRepository.save(calendar);
            log.info("캘린더 일정 업데이트 완료: {}", calendar.getId());

        } else {
            // [저장 로직] 존재하지 않으면, 새 일정으로 생성
            Calendar newCalendar = Calendar.builder()
                    .originId(dto.getOriginId()) // originId (referenceId) 저장
                    .memberId(dto.getMemberId())
                    .title(dto.getTitle())
                    .contents(dto.getContents())
                    .type(CalendarType.fromCode(dto.getType()))
                    .startDate(dto.getStartDate())
                    .endDate(dto.getEndDate())
                    .build();
            calendarRepository.save(newCalendar);
            log.info("새 캘린더 일정 생성 완료: {}", newCalendar.getId());
        }
    }

//    개인 일정 외 일정 삭제
    public void deleteSchedule(ScheduleDeleteDto dto) {
        List<Calendar> calendarList = calendarRepository.findByOriginId(dto.getOriginId());
        for(Calendar c : calendarList) {
            c.deleteSchedule();
        }
    }
}
