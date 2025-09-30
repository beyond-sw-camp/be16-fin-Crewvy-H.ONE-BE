package com.crewvy.workforce_service.reservation.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workforce_service.reservation.dto.request.ReservationCategoryCreateReq;
import com.crewvy.workforce_service.reservation.dto.response.ReservationCategoryCreateRes;
import com.crewvy.workforce_service.reservation.service.ReservationCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reservation/category")
@RequiredArgsConstructor
@Slf4j
public class ReservationCategoryController {

    private final ReservationCategoryService reservationCategoryService;

    @PostMapping("/create")
    public ApiResponse<?> create(@RequestBody ReservationCategoryCreateReq reservationCategoryCreateReq) {

        ReservationCategoryCreateRes res = reservationCategoryService.saveReservationCategory(reservationCategoryCreateReq);
        return new ApiResponse<>(true, res, "예약 카테고리 추가 성공");
    }
}
