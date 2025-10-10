package com.crewvy.workforce_service.reservation.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workforce_service.reservation.dto.request.ReservationCategoryCreateReq;
import com.crewvy.workforce_service.reservation.dto.request.ReservationCategoryUpdateReq;
import com.crewvy.workforce_service.reservation.dto.response.ReservationCategoryCreateRes;
import com.crewvy.workforce_service.reservation.dto.response.ReservationCategoryListRes;
import com.crewvy.workforce_service.reservation.dto.response.ReservationCategoryUpdateRes;
import com.crewvy.workforce_service.reservation.service.ReservationCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/reservation/category")
@RequiredArgsConstructor
@Slf4j
public class ReservationCategoryController {

    private final ReservationCategoryService reservationCategoryService;

    @PostMapping("/register")
    public ApiResponse<?> create(@RequestBody ReservationCategoryCreateReq reservationCategoryCreateReq) {
        ReservationCategoryCreateRes res = reservationCategoryService.saveReservationCategory(reservationCategoryCreateReq);
        return new ApiResponse<>(true, res, "예약 카테고리 추가 성공");
    }

    // 카테고리 조회
    @GetMapping("/list")
    public ApiResponse<?> list(@RequestParam UUID companyId) {
        List<ReservationCategoryListRes> res = reservationCategoryService.listByCompany(companyId);
        return new ApiResponse<>(true, res, "예약 카테고리 조회 성공");
    }

    // 카테고리 수정
    @PutMapping("/update/{id}")
    public ApiResponse<?> update(@PathVariable UUID id, @RequestBody ReservationCategoryUpdateReq req) {
        ReservationCategoryUpdateRes res = reservationCategoryService.update(id, req);
        return new ApiResponse<>(true, res, "예약 카테고리 수정 성공");
    }

    // 카테고리 삭제
    @DeleteMapping("/delete/{id}")
    public ApiResponse<?> delete(@PathVariable UUID id) {
        reservationCategoryService.delete(id);
        return new ApiResponse<>(true, null, "예약 카테고리 삭제 성공");
    }
}
