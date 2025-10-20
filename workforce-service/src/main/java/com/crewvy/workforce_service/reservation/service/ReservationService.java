package com.crewvy.workforce_service.reservation.service;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.common.entity.Bool;
import com.crewvy.common.exception.ResourceNotFoundException;
import com.crewvy.workforce_service.feignClient.MemberClient;
import com.crewvy.workforce_service.feignClient.dto.request.IdListReq;
import com.crewvy.workforce_service.feignClient.dto.response.NameDto;
import com.crewvy.workforce_service.reservation.constant.DayOfWeek;
import com.crewvy.workforce_service.reservation.dto.request.ReservationCreateReq;
import com.crewvy.workforce_service.reservation.dto.request.ReservationUpdateReq;
import com.crewvy.workforce_service.reservation.dto.request.ReservationUpdateStatusReq;
import com.crewvy.workforce_service.reservation.dto.response.ReservationRes;
import com.crewvy.workforce_service.reservation.entity.RecurringSetting;
import com.crewvy.workforce_service.reservation.entity.Reservation;
import com.crewvy.workforce_service.reservation.entity.ReservationType;
import com.crewvy.workforce_service.reservation.repository.RecurringSettingRepository;
import com.crewvy.workforce_service.reservation.repository.ReservationRepository;
import com.crewvy.workforce_service.reservation.repository.ReservationTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationTypeRepository reservationTypeRepository;
    private final RecurringSettingRepository recurringSettingRepository;
    private final MemberClient memberClient;

    public ReservationRes create(ReservationCreateReq reservationCreateReq) {
        ReservationType type = reservationTypeRepository.findWithLockById(reservationCreateReq.getReservationTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("예약 자원을 찾을 수 없습니다."));

        if (reservationCreateReq.getIsRepeated() == Bool.TRUE && reservationCreateReq.getRepeatCreateReq() != null) {
            return createRecurringReservations(reservationCreateReq, type);
        } else {
            return createSingleReservation(reservationCreateReq, type);
        }
    }

    private ReservationRes createSingleReservation(ReservationCreateReq reservationCreateReq, ReservationType type) {

        checkOverlapping(reservationCreateReq.getReservationTypeId()
                , reservationCreateReq.getStartDateTime()
                , reservationCreateReq.getEndDateTime());

        Reservation reservation = reservationCreateReq.toEntity(type);
        Reservation savedReservation = reservationRepository.save(reservation);
        return ReservationRes.fromEntity(savedReservation);
    }

    private ReservationRes createRecurringReservations(ReservationCreateReq reservationCreateReq, ReservationType type) {
        RecurringSetting recurringSetting = reservationCreateReq.getRepeatCreateReq().toEntity();
        RecurringSetting saved = recurringSettingRepository.save(recurringSetting);


        List<LocalDateTime> recurringStartTimes = calculateRecurringDates(reservationCreateReq);
        if (recurringStartTimes.isEmpty()) {
            throw new IllegalArgumentException("반복될 예약 날짜가 없습니다.");
        }

        List<Reservation> reservationsList = new ArrayList<>();

        Duration duration = Duration.between(reservationCreateReq.getStartDateTime(), reservationCreateReq.getEndDateTime());

        for (LocalDateTime startTime : recurringStartTimes) {

            LocalDateTime endTime = startTime.plus(duration);
            checkOverlapping(reservationCreateReq.getReservationTypeId(), startTime, endTime);
            Reservation reservation = reservationCreateReq.toEntity(type, startTime, endTime);
            reservation.setRecurringSetting(saved);
            reservationsList.add(reservation);
        }

        List<Reservation> savedReservations = reservationRepository.saveAll(reservationsList);

        return ReservationRes.fromEntity(savedReservations.get(0));
    }


    private void checkOverlapping(UUID typeId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        boolean isOverlapping = reservationRepository.existsByReservationTypeIdAndStartDateTimeLessThanAndEndDateTimeGreaterThan(
                typeId, endDateTime, startDateTime);
        if (isOverlapping) {
            throw new IllegalArgumentException("해당 시간대에 이미 예약된 자원이 있습니다: " + startDateTime);
        }
    }

    private List<LocalDateTime> calculateRecurringDates(ReservationCreateReq req) {
        List<LocalDateTime> dates = new ArrayList<>();
        LocalDateTime currentDateTime = req.getStartDateTime();
        LocalDate endDate = req.getRepeatCreateReq().getEndDate();

        int dayOfWeekMask = 0;
        List<DayOfWeek> selectedDays = req.getRepeatCreateReq().getDayOfWeek();
        if (selectedDays != null && !selectedDays.isEmpty()) {
            for (DayOfWeek day : selectedDays) {
                dayOfWeekMask |= day.getCodeValue();
            }
        }

        while (!currentDateTime.toLocalDate().isAfter(endDate)) {
            java.time.DayOfWeek currentDay = currentDateTime.getDayOfWeek();
            int currentDayBitValue = mapJavaDayToCustomBit(currentDay);

            if ((dayOfWeekMask & currentDayBitValue) > 0) {
                dates.add(currentDateTime);
            }
            currentDateTime = currentDateTime.plusDays(1);
        }
        return dates;
    }

    private int mapJavaDayToCustomBit(java.time.DayOfWeek javaDay) {
        return switch (javaDay) {
            case SUNDAY -> DayOfWeek.SUNDAY.getCodeValue();
            case MONDAY -> DayOfWeek.MONDAY.getCodeValue();
            case TUESDAY -> DayOfWeek.TUESDAY.getCodeValue();
            case WEDNESDAY -> DayOfWeek.WEDNESDAY.getCodeValue();
            case THURSDAY -> DayOfWeek.THURSDAY.getCodeValue();
            case FRIDAY -> DayOfWeek.FRIDAY.getCodeValue();
            case SATURDAY -> DayOfWeek.SATURDAY.getCodeValue();
        };
    }

    @Transactional(readOnly = true)
    public List<ReservationRes> listByCompany(UUID memberPositionId, UUID companyId) {
        List<ReservationRes> response = reservationRepository.findByCompanyId(companyId)
                .stream()
                .map(ReservationRes::fromEntity)
                .toList();

        if (response.isEmpty()) {
            return response;
        }

        IdListReq idListReq = new IdListReq(response.stream().map(ReservationRes::getMemberId).distinct().toList());
        ApiResponse<List<NameDto>> nameDtoApiResponse = memberClient.getNameList(memberPositionId, idListReq);
        List<NameDto> nameDtoList = nameDtoApiResponse.getData();

        Map<UUID, String> memberIdToNameMap = nameDtoList.stream()
                .collect(Collectors.toMap(NameDto::getMemberId, NameDto::getName));

        response.forEach(reservationRes -> {
            String memberName = memberIdToNameMap.get(reservationRes.getMemberId());

            if (memberName != null) {
                reservationRes.setName(memberName);
            }
        });

        return response;
    }

    @Transactional(readOnly = true)
    public List<ReservationRes> listByCompanyAndMember(UUID companyId, UUID memberId) {

        return reservationRepository.findByCompanyIdAndMemberId(companyId, memberId)
                .stream().map(ReservationRes::fromEntity)
                .collect(Collectors.toList());
    }

    public ReservationRes update(UUID id, ReservationUpdateReq req) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("예약을 찾을 수 없습니다."));

        if (req.getStartDateTime() != null) {
            reservation.setStartDateTime(req.getStartDateTime());
        }

        if (req.getEndDateTime() != null) {
            reservation.setEndDateTime(req.getEndDateTime());
        }
        if (req.getStatus() != null) {
            reservation.setStatus(req.getStatus());
        }

        Reservation saved = reservationRepository.save(reservation);
        return ReservationRes.fromEntity(saved);
    }

    public void delete(UUID id) {
        if (!reservationRepository.existsById(id)) {
            throw new ResourceNotFoundException("예약을 찾을 수 없습니다.");
        }
        reservationRepository.deleteById(id);
    }

    public ReservationRes updateReservationStatus(UUID id, ReservationUpdateStatusReq req) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("예약을 찾을 수 없습니다."));

        reservation.setStatus(req.getReservationStatus());
        Reservation saved = reservationRepository.save(reservation);
        return ReservationRes.fromEntity(saved);
    }
}


