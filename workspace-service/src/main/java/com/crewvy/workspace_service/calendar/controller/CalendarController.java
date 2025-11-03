package com.crewvy.workspace_service.calendar.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workspace_service.calendar.dto.request.PersonalScheduleReqDto;
import com.crewvy.workspace_service.calendar.service.CalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/calendar")
@RequiredArgsConstructor
public class CalendarController {
    private final CalendarService calendarService;

    @GetMapping("/find-my-schedule")
    public ResponseEntity<?> findMySchedule(@RequestHeader("X-User-UUID") UUID memberId,
                                            @RequestParam String searchType,
                                            @RequestParam(required = false) Integer year,
                                            @RequestParam(required = false) Integer month
    ) {
        return new ResponseEntity<>(
                ApiResponse.success(calendarService.findMySchedule(memberId, searchType, year, month), "내 일정 찾기"),
                HttpStatus.OK
        );
    }

    @PostMapping("/post-my-schedule")
    public ResponseEntity<?> postMySchedule(@RequestHeader("X-User-UUID") UUID memberId,
                                            @RequestBody PersonalScheduleReqDto scheduleDto
    ) {
        return new ResponseEntity<>(
                ApiResponse.success(calendarService.postMySchedule(memberId, scheduleDto), "개인 일정 등록"),
                HttpStatus.CREATED
        );
    }

    @PatchMapping("/update-my-schedule/{id}")
    public ResponseEntity<?> updateMySchedule(@RequestBody PersonalScheduleReqDto dto,
                                              @PathVariable UUID id
    ) {
        calendarService.updateMySchedule(id, dto);
        return new ResponseEntity<>(ApiResponse.success(null, "내 일정 업데이트"),
                HttpStatus.OK
        );
    }

    @DeleteMapping("/delete-my-schedule/{id}")
    public ResponseEntity<?> deleteMySchedule(@PathVariable UUID id) {
        calendarService.deleteMySchedule(id);
        return new ResponseEntity<>(ApiResponse.success(null, "내 일정 삭제"),
                HttpStatus.OK
        );
    }
}
