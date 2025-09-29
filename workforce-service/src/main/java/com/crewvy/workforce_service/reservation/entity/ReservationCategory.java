package com.crewvy.workforce_service.reservation.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
public class ReservationCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private UUID companyId;

}
