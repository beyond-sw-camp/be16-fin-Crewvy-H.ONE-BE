package com.crewvy.workforce_service.reservation.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
public class ReservationType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT), nullable = false)
    private ReservationCategory reservationCategory;

    @Column(nullable = false)
    private String name;

    private int capacity;

    private String facilities;

    private String description;

}
