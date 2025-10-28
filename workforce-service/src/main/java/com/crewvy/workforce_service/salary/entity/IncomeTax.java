package com.crewvy.workforce_service.salary.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Entity
public class IncomeTax {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private long incomeStart;

    @Column(nullable = false)
    private long incomeEnd;

    @Column(nullable = false)
    private int dependentCount;

    @Column(nullable = false)
    private long taxAmount;
}
