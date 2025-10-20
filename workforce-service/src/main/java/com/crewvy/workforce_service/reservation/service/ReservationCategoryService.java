package com.crewvy.workforce_service.reservation.service;

import com.crewvy.common.exception.ResourceNotFoundException;
import com.crewvy.workforce_service.reservation.dto.request.ReservationCategoryCreateReq;
import com.crewvy.workforce_service.reservation.dto.request.ReservationCategoryUpdateReq;
import com.crewvy.workforce_service.reservation.dto.response.ReservationCategoryCreateRes;
import com.crewvy.workforce_service.reservation.dto.response.ReservationCategoryListRes;
import com.crewvy.workforce_service.reservation.dto.response.ReservationCategoryUpdateRes;
import com.crewvy.workforce_service.reservation.entity.ReservationCategory;
import com.crewvy.workforce_service.reservation.repository.ReservationCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

    public List<ReservationCategoryListRes> listByCompany(UUID companyId) {
        return reservationCategoryRepository.findByCompanyId(companyId)
                .stream().map(ReservationCategoryListRes::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReservationCategoryUpdateRes update(UUID id, ReservationCategoryUpdateReq req) {
        ReservationCategory category = reservationCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("예약 카테고리를 찾을 수 없습니다."));

        category.update(req);

        return ReservationCategoryUpdateRes.fromEntity(category);
    }

    public void delete(UUID id) {
        if (!reservationCategoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("예약 카테고리를 찾을 수 없습니다.");
        }
        reservationCategoryRepository.deleteById(id);
    }
}
