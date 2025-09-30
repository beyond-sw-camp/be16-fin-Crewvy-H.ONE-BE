package com.crewvy.workforce_service.reservation.service;

import com.crewvy.workforce_service.reservation.dto.request.ReservationCategoryCreateReq;
import com.crewvy.workforce_service.reservation.dto.response.ReservationCategoryCreateRes;
import com.crewvy.workforce_service.reservation.entity.ReservationCategory;
import com.crewvy.workforce_service.reservation.repository.ReservationCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ReservationCategoryService {

    private final ReservationCategoryRepository reservationCategoryRepository;

    public ReservationCategoryCreateRes saveReservationCategory(ReservationCategoryCreateReq reservationCategoryCreateReq) {
        ReservationCategory reservationCategory = reservationCategoryRepository.save(reservationCategoryCreateReq.toEntity());
        return ReservationCategoryCreateRes.fromEntity(reservationCategory);
    }
}
