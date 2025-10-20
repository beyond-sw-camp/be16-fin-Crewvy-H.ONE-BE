package com.crewvy.workforce_service.reservation.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workforce_service.reservation.dto.request.ReservationCreateReq;
import com.crewvy.workforce_service.reservation.dto.request.ReservationUpdateReq;
import com.crewvy.workforce_service.reservation.dto.request.ReservationUpdateStatusReq;
import com.crewvy.workforce_service.reservation.dto.response.ReservationRes;
import com.crewvy.workforce_service.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/reservation")
@RequiredArgsConstructor
@Slf4j
public class ReservationController {

    private final ReservationService reservationService;

    // 예약 등록
    @PostMapping("/register")
    public ResponseEntity<?> create(@RequestBody ReservationCreateReq req) {
        log.error("예약 확인 : {}", req);
        ReservationRes res = reservationService.create(req);
        return new ResponseEntity<>(new ApiResponse<>(true, res, "예약 등록 성공"), HttpStatus.CREATED);
    }

    // 전체 예약 조회
    @GetMapping("/list")
    public ResponseEntity<?> allReservationList(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId, @RequestParam UUID companyId) {
        List<ReservationRes> res = reservationService.listByCompany(memberPositionId, companyId);
        return new ResponseEntity<>(new ApiResponse<>(true, res, "예약 조회 성공"), HttpStatus.OK);
    }

    // 내 예약 조회
    @GetMapping("/myList")
    public ResponseEntity<?> myReservationList(@RequestParam UUID companyId,
                                            @RequestParam UUID memberId) {
        List<ReservationRes> res = reservationService.listByCompanyAndMember(companyId, memberId);
        return new ResponseEntity<>(new ApiResponse<>(true, res, "내 예약 조회 성공"), HttpStatus.OK);
    }

    // 예약 수정
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody ReservationUpdateReq req) {
        ReservationRes res = reservationService.update(id, req);
        return new ResponseEntity<>(new ApiResponse<>(true, res, "예약 수정 성공"), HttpStatus.OK);
    }

    // 예약 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        reservationService.delete(id);
        return new ResponseEntity<>(new ApiResponse<>(true, null, "예약 삭제 성공"), HttpStatus.OK);
    }
    
    // 예약 상태 변경
    @PutMapping("/status/{id}")
    public ResponseEntity<?> updateReservationStatus(@PathVariable UUID id, @RequestBody ReservationUpdateStatusReq req) {
        ReservationRes res = reservationService.updateReservationStatus(id, req);
        return new ResponseEntity<>(new ApiResponse<>(true, res, "예약 상태 변경 성공"), HttpStatus.OK);
    }
}
