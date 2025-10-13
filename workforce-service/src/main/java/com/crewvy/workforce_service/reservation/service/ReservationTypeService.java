package com.crewvy.workforce_service.reservation.service;

import com.crewvy.workforce_service.reservation.dto.request.ReservationTypeCreateReq;
import com.crewvy.workforce_service.reservation.dto.request.ReservationTypeUpdateReq;
import com.crewvy.workforce_service.reservation.dto.response.ReservationTypeCreateRes;
import com.crewvy.workforce_service.reservation.dto.response.ReservationTypeListRes;
import com.crewvy.workforce_service.reservation.dto.response.ReservationTypeUpdateRes;
import com.crewvy.workforce_service.reservation.entity.ReservationType;
import com.crewvy.workforce_service.reservation.repository.ReservationTypeRepository;
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
public class ReservationTypeService {

    private final ReservationTypeRepository reservationTypeRepository;

    public ReservationTypeCreateRes saveReservationType(ReservationTypeCreateReq reservationTypeCreateReq) {
        ReservationType reservationType = reservationTypeRepository.save(reservationTypeCreateReq.toEntity());
        return ReservationTypeCreateRes.fromEntity(reservationType);
    }

    public List<ReservationTypeListRes> getResourceList(UUID companyId) {
        List<ReservationType> reservationTypes = reservationTypeRepository.findByCompanyId(companyId);
        log.info("조회된 예약 자원 수: {}", reservationTypes.size());
        
        return reservationTypes.stream()
                .map(reservationType -> {
                    log.info("예약 자원 ID: {}, 카테고리: {}", 
                            reservationType.getId(), 
                            reservationType.getReservationCategory() != null ? 
                                    reservationType.getReservationCategory().getName() : "null");
                    return ReservationTypeListRes.fromEntity(reservationType);
                })
                .collect(Collectors.toList());
    }

    public ReservationTypeUpdateRes updateReservationType(UUID id, ReservationTypeUpdateReq updateReq) {
        ReservationType reservationType = reservationTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("예약 자원을 찾을 수 없습니다."));
        
        if (updateReq.getName() != null) {
            reservationType.setName(updateReq.getName());
        }
        if (updateReq.getLocation() != null) {
            reservationType.setLocation(updateReq.getLocation());
        }
        if (updateReq.getCapacity() != 0) {
            reservationType.setCapacity(updateReq.getCapacity());
        }
        if (updateReq.getFacilities() != null) {
            reservationType.setFacilities(updateReq.getFacilities());
        }
        if (updateReq.getDescription() != null) {
            reservationType.setDescription(updateReq.getDescription());
        }
        if (updateReq.getReservationCategoryStatus() != null) {
            reservationType.setReservationCategoryStatus(updateReq.getReservationCategoryStatus());
        }
        
        ReservationType updatedReservationType = reservationTypeRepository.save(reservationType);
        return ReservationTypeUpdateRes.fromEntity(updatedReservationType);
    }

    public void deleteReservationType(UUID id) {
        if (!reservationTypeRepository.existsById(id)) {
            throw new RuntimeException("예약 자원을 찾을 수 없습니다.");
        }
        reservationTypeRepository.deleteById(id);
    }
}
