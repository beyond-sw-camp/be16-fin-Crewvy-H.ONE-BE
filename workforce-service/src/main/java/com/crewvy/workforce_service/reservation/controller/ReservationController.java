package com.crewvy.workforce_service.reservation.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workforce_service.reservation.dto.request.ReservationCreateReq;
import com.crewvy.workforce_service.reservation.dto.request.ReservationUpdateReq;
import com.crewvy.workforce_service.reservation.dto.response.ReservationRes;
import com.crewvy.workforce_service.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public ApiResponse<?> create(@RequestBody ReservationCreateReq req) {
        ReservationRes res = reservationService.create(req);
        return new ApiResponse<>(true, res, "예약 등록 성공");
    }

    // 전체 예약 조회
    @GetMapping("/list")
    public ApiResponse<?> allReservationList(@RequestParam UUID companyId) {
        List<ReservationRes> res = reservationService.listByCompany(companyId);
        return new ApiResponse<>(true, res, "예약 조회 성공");
    }

    // 내 예약 조회
    @GetMapping("/myList")
    public ApiResponse<?> myReservationList(@RequestParam UUID companyId,
                                            @RequestParam UUID memberId) {
        List<ReservationRes> res = reservationService.listByCompanyAndMember(companyId, memberId);
        return new ApiResponse<>(true, res, "내 예약 조회 성공");
    }

    // 예약 수정
    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable UUID id, @RequestBody ReservationUpdateReq req) {
        ReservationRes res = reservationService.update(id, req);
        return new ApiResponse<>(true, res, "예약 수정 성공");
    }

    // 예약 삭제
    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable UUID id) {
        reservationService.delete(id);
        return new ApiResponse<>(true, null, "예약 삭제 성공");
    }
}
