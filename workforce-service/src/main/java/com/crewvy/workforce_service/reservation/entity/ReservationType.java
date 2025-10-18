package com.crewvy.workforce_service.reservation.entity;

import com.crewvy.common.entity.BaseEntity;
import com.crewvy.workforce_service.reservation.constant.ReservationCategoryStatus;
import com.crewvy.workforce_service.reservation.converter.ReservationCategoryStatusConverter;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Entity
@Setter
public class ReservationType extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT), nullable = false)
    private ReservationCategory reservationCategory;

    @Column(nullable = false)
    @Convert(converter = ReservationCategoryStatusConverter.class)
    private ReservationCategoryStatus reservationCategoryStatus;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String location;

    private int capacity;

    private String facilities;

    private String description;

}
