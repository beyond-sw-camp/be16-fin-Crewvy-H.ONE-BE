package com.crewvy.workforce_service.attendance.batch;

import com.crewvy.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 근태 배치 수동 실행 컨트롤러
 * 테스트 및 관리자용
 */
@Slf4j
@RestController
@RequestMapping("/batch/attendance")
@RequiredArgsConstructor
public class AttendanceBatchController {

    private final AttendanceBatchScheduler batchScheduler;

    /**
     * 결근 처리 배치 수동 실행
     */
    @PostMapping("/mark-absent")
    public ResponseEntity<ApiResponse<Void>> runMarkAbsentBatch() {
        log.info("결근 처리 배치 수동 실행 요청");
        try {
            batchScheduler.runMarkAbsentJobManually();
            return new ResponseEntity<>(
                    ApiResponse.success(null, "결근 처리 배치가 실행되었습니다."),
                    HttpStatus.OK
            );
        } catch (Exception e) {
            log.error("결근 처리 배치 수동 실행 실패", e);
            return new ResponseEntity<>(
                    ApiResponse.error("배치 실행 중 오류가 발생했습니다: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * 휴가 DailyAttendance 생성 배치 수동 실행
     */
    @PostMapping("/create-leave-attendance")
    public ResponseEntity<ApiResponse<Void>> runCreateLeaveDailyAttendanceBatch() {
        log.info("휴가 DailyAttendance 생성 배치 수동 실행 요청");
        try {
            batchScheduler.runCreateLeaveDailyAttendanceJobManually();
            return new ResponseEntity<>(
                    ApiResponse.success(null, "휴가 DailyAttendance 생성 배치가 실행되었습니다."),
                    HttpStatus.OK
            );
        } catch (Exception e) {
            log.error("휴가 DailyAttendance 생성 배치 수동 실행 실패", e);
            return new ResponseEntity<>(
                    ApiResponse.error("배치 실행 중 오류가 발생했습니다: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * 미완료 퇴근 자동 처리 배치 수동 실행
     */
    @PostMapping("/auto-complete-clock-out")
    public ResponseEntity<ApiResponse<Void>> runAutoCompleteClockOutBatch() {
        log.info("미완료 퇴근 자동 처리 배치 수동 실행 요청");
        try {
            batchScheduler.runAutoCompleteClockOutJobManually();
            return new ResponseEntity<>(
                    ApiResponse.success(null, "미완료 퇴근 자동 처리 배치가 실행되었습니다."),
                    HttpStatus.OK
            );
        } catch (Exception e) {
            log.error("미완료 퇴근 자동 처리 배치 수동 실행 실패", e);
            return new ResponseEntity<>(
                    ApiResponse.error("배치 실행 중 오류가 발생했습니다: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * 모든 일일 배치 수동 실행
     */
    @PostMapping("/run-daily")
    public ResponseEntity<ApiResponse<Void>> runDailyBatch() {
        log.info("일일 배치 전체 수동 실행 요청");
        try {
            batchScheduler.runDailyAttendanceBatch();
            return new ResponseEntity<>(
                    ApiResponse.success(null, "일일 배치가 실행되었습니다."),
                    HttpStatus.OK
            );
        } catch (Exception e) {
            log.error("일일 배치 수동 실행 실패", e);
            return new ResponseEntity<>(
                    ApiResponse.error("배치 실행 중 오류가 발생했습니다: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    /**
     * 연차 자동 발생 배치 수동 실행
     */
    @PostMapping("/annual-leave-accrual")
    public ResponseEntity<ApiResponse<Void>> runAnnualLeaveAccrualBatch() {
        log.info("연차 자동 발생 배치 수동 실행 요청");
        try {
            batchScheduler.runAnnualLeaveAccrualJobManually();
            return new ResponseEntity<>(
                    ApiResponse.success(null, "연차 자동 발생 배치가 실행되었습니다."),
                    HttpStatus.OK
            );
        } catch (Exception e) {
            log.error("연차 자동 발생 배치 수동 실행 실패", e);
            return new ResponseEntity<>(
                    ApiResponse.error("배치 실행 중 오류가 발생했습니다: " + e.getMessage()),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
}
