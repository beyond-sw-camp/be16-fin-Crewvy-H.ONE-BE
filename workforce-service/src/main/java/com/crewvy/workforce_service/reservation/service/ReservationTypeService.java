package com.crewvy.workforce_service.reservation.service;

import com.crewvy.common.exception.ResourceNotFoundException;
import com.crewvy.workforce_service.reservation.constant.ReservationTypeStatus;
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
        return reservationTypes.stream()
                .map(ReservationTypeListRes::fromEntity)
                .collect(Collectors.toList());
    }

    public List<ReservationTypeListRes> getAvailableResourceList(UUID companyId) {
        List<ReservationTypeStatus> statuses = List.of(ReservationTypeStatus.AVAILABLE, ReservationTypeStatus.RESERVED);
        List<ReservationType> reservationTypes = reservationTypeRepository.findByCompanyIdAndReservationTypeStatusIn(companyId, statuses);

        return reservationTypes.stream()
                .map(ReservationTypeListRes::fromEntity)
                .collect(Collectors.toList());
    }

    public ReservationTypeUpdateRes updateReservationType(UUID id, ReservationTypeUpdateReq updateReq) {

        ReservationType reservationType = reservationTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("예약 자원을 찾을 수 없습니다."));
        
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
        if (updateReq.getReservationTypeStatus() != null) {
            reservationType.setReservationTypeStatus(updateReq.getReservationTypeStatus());
        }
        
        ReservationType updatedReservationType = reservationTypeRepository.save(reservationType);
        return ReservationTypeUpdateRes.fromEntity(updatedReservationType);
    }

    public void deleteReservationType(UUID id) {
        if (!reservationTypeRepository.existsById(id)) {
            throw new ResourceNotFoundException("예약 자원을 찾을 수 없습니다.");
        }
        reservationTypeRepository.deleteById(id);
    }

}
