package com.crewvy.workforce_service.reservation.entity;

import com.crewvy.common.entity.BaseEntity;
import com.crewvy.common.entity.Bool;
import com.crewvy.workforce_service.reservation.constant.ReservationRequestStatus;
import com.crewvy.workforce_service.reservation.constant.ReservationStatus;
import com.crewvy.workforce_service.reservation.converter.ReservationRequestStatusConverter;
import com.crewvy.workforce_service.reservation.converter.ReservationStatusConverter;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
public class Reservation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT), nullable = false)
    private ReservationType reservationType;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    @Convert(converter = ReservationRequestStatusConverter.class)
    private ReservationRequestStatus requestStatus;

    @Column(nullable = false)
    @Convert(converter = ReservationStatusConverter.class)
    private ReservationStatus status;
    
    private LocalDateTime startDateTime;

    private LocalDateTime endDateTime;

    private String title;

    private int number;

    private String note;

    private int participant;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Bool isRepeated;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurring_setting_id")
    private RecurringSetting recurringSetting;

}
