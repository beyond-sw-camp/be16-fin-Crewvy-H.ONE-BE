package com.crewvy.workforce_service.reservation.service;

import com.crewvy.workforce_service.reservation.dto.request.ReservationCreateReq;
import com.crewvy.workforce_service.reservation.dto.request.ReservationUpdateReq;
import com.crewvy.workforce_service.reservation.dto.response.ReservationRes;
import com.crewvy.workforce_service.reservation.entity.Reservation;
import com.crewvy.workforce_service.reservation.entity.ReservationType;
import com.crewvy.workforce_service.reservation.repository.ReservationRepository;
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
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationTypeRepository reservationTypeRepository;

    public ReservationRes create(ReservationCreateReq req) {
        ReservationType type = reservationTypeRepository.findById(req.getReservationTypeId())
                .orElseThrow(() -> new RuntimeException("예약 자원(타입)을 찾을 수 없습니다."));
        Reservation saved = reservationRepository.save(req.toEntity(type));
        return ReservationRes.fromEntity(saved);
    }

    public List<ReservationRes> listByCompany(UUID companyId) {
        return reservationRepository.findByCompanyId(companyId)
                .stream().map(ReservationRes::fromEntity)
                .collect(Collectors.toList());
    }

    public List<ReservationRes> listByCompanyAndMember(UUID companyId, UUID memberId) {
        return reservationRepository.findByCompanyIdAndMemberId(companyId, memberId)
                .stream().map(ReservationRes::fromEntity)
                .collect(Collectors.toList());
    }

    public ReservationRes update(UUID id, ReservationUpdateReq req) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("예약을 찾을 수 없습니다."));

        if (req.getStartDateTime() != null) reservation.setStartDateTime(req.getStartDateTime());
        if (req.getEndDateTime() != null) reservation.setEndDateTime(req.getEndDateTime());
        if (req.getStatus() != null) reservation.setStatus(req.getStatus());

        Reservation saved = reservationRepository.save(reservation);
        return ReservationRes.fromEntity(saved);
    }

    public void delete(UUID id) {
        if (!reservationRepository.existsById(id)) {
            throw new RuntimeException("예약을 찾을 수 없습니다.");
        }
        reservationRepository.deleteById(id);
    }
}


