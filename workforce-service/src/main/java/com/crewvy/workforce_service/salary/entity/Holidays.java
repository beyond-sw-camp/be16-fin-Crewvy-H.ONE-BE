package com.crewvy.workforce_service.salary.entity;

import com.crewvy.common.entity.Bool;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Entity
public class Holidays {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private LocalDate lunarDate;

    @Column(nullable = false)
    private LocalDate solarDate;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Bool yun;

    @Column(nullable = false)
    private String memo;

}
