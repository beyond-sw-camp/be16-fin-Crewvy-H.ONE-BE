package com.crewvy.workforce_service.reservation.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workforce_service.reservation.dto.request.ReservationTypeCreateReq;
import com.crewvy.workforce_service.reservation.dto.request.ReservationTypeUpdateReq;
import com.crewvy.workforce_service.reservation.dto.response.ReservationTypeCreateRes;
import com.crewvy.workforce_service.reservation.dto.response.ReservationTypeListRes;
import com.crewvy.workforce_service.reservation.dto.response.ReservationTypeUpdateRes;
import com.crewvy.workforce_service.reservation.service.ReservationTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/reservation/type")
@RequiredArgsConstructor
@Slf4j
public class ReservationTypeController {

    private final ReservationTypeService reservationTypeService;
    
    // 예약 자원 추가
    @PostMapping("/register")
    public ResponseEntity<?> createResource(@RequestBody ReservationTypeCreateReq reservationTypeCreateReq) {
        ReservationTypeCreateRes res = reservationTypeService.saveReservationType(reservationTypeCreateReq);
        return new ResponseEntity<>(new ApiResponse<>(true, res, "예약 자원 추가 성공"), HttpStatus.CREATED);
    }

    // 예약 자원 조회
    @GetMapping("/list")
    public ResponseEntity<?> getResourceList(@RequestParam UUID companyId) {
        List<ReservationTypeListRes> res = reservationTypeService.getResourceList(companyId);
        return new ResponseEntity<>(new ApiResponse<>(true, res, "예약 자원 조회 성공"), HttpStatus.OK);
    }

    // 예약 자원 수정
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateResource(@PathVariable UUID id, @RequestBody ReservationTypeUpdateReq updateReq) {
        ReservationTypeUpdateRes res = reservationTypeService.updateReservationType(id, updateReq);
        return new ResponseEntity<>(new ApiResponse<>(true, res, "예약 자원 수정 성공"), HttpStatus.OK);
    }

    // 예약 자원 삭제
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteResource(@PathVariable UUID id) {
        reservationTypeService.deleteReservationType(id);
        return new ResponseEntity<>(new ApiResponse<>(true, null, "예약 자원 삭제 성공"), HttpStatus.OK);
    }
}
