package com.crewvy.workforce_service.reservation.entity;

import com.crewvy.common.entity.BaseEntity;
import com.crewvy.workforce_service.reservation.constant.ReservationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Entity
public class Reservation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "type_id", nullable = false)
    private ReservationType reservationType;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    @Convert(converter = ReservationStatus.class)
    private ReservationStatus status;

    private LocalDateTime startDateTime;

    private LocalDateTime endDateTime;

}
