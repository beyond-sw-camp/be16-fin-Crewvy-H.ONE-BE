package com.crewvy.workforce_service.reservation.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workforce_service.reservation.constant.ReservationTypeStatus;
import com.crewvy.workforce_service.reservation.dto.request.ReservationTypeCreateReq;
import com.crewvy.workforce_service.reservation.dto.request.ReservationTypeUpdateReq;
import com.crewvy.workforce_service.reservation.dto.response.ReservationEnumRes;
import com.crewvy.workforce_service.reservation.dto.response.ReservationTypeCreateRes;
import com.crewvy.workforce_service.reservation.dto.response.ReservationTypeListRes;
import com.crewvy.workforce_service.reservation.dto.response.ReservationTypeUpdateRes;
import com.crewvy.workforce_service.reservation.service.ReservationTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/reservation/type")
@RequiredArgsConstructor
@Slf4j
public class ReservationTypeController {

    private final ReservationTypeService reservationTypeService;
    
    // 예약 자원 추가
    @PostMapping("/register")
    public ResponseEntity<?> createResource(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                            @RequestHeader("X-User-CompanyId") UUID companyId,
                                            @RequestBody ReservationTypeCreateReq reservationTypeCreateReq) {
        ReservationTypeCreateRes res = reservationTypeService.saveReservationType(memberPositionId, companyId,
                reservationTypeCreateReq);
        return new ResponseEntity<>(new ApiResponse<>(true, res, "예약 자원 추가 성공"), HttpStatus.CREATED);
    }

    // 전체 예약 자원 조회 (관리자)
    @GetMapping("/list")
    public ResponseEntity<?> getResourceList(@RequestHeader("X-User-CompanyId") UUID companyId) {
        List<ReservationTypeListRes> res = reservationTypeService.getResourceList(companyId);
        return new ResponseEntity<>(new ApiResponse<>(true, res, "예약 자원 조회 성공"), HttpStatus.OK);
    }

    // 사용 가능 예약 자원 조회 (사용자)
    @GetMapping("/list-available")
    public ResponseEntity<?> getAvailableResourceList(@RequestHeader("X-User-CompanyId") UUID companyId) {
        List<ReservationTypeListRes> res = reservationTypeService.getAvailableResourceList(companyId);
        return new ResponseEntity<>(new ApiResponse<>(true, res, "예약 자원 조회 성공"), HttpStatus.OK);
    }

    // 예약 자원 수정
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateResource(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                            @PathVariable UUID id, @RequestBody ReservationTypeUpdateReq updateReq) {
        ReservationTypeUpdateRes res = reservationTypeService.updateReservationType(memberPositionId, id, updateReq);
        return new ResponseEntity<>(new ApiResponse<>(true, res, "예약 자원 수정 성공"), HttpStatus.OK);
    }

    // 예약 자원 삭제
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteResource(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                            @PathVariable UUID id) {
        reservationTypeService.deleteReservationType(memberPositionId, id);
        return new ResponseEntity<>(new ApiResponse<>(true, null, "예약 자원 삭제 성공"),
                HttpStatus.OK);
    }

    @GetMapping("/status-list")
    public ResponseEntity<?> getResourceStatusList(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId) {
        List<ReservationEnumRes> statusList = Arrays.stream(ReservationTypeStatus.values())
                .map(status -> new ReservationEnumRes(status.name(), status.getCodeValue(),
                        status.getCodeName()))
                .collect(Collectors.toList());
        return new ResponseEntity<>(new ApiResponse<>(true, statusList, "자원 상태 조회 성공"),
                HttpStatus.OK);
    }
}
