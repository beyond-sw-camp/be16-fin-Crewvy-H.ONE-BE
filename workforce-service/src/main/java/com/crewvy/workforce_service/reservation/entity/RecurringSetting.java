package com.crewvy.workforce_service.reservation.entity;

import com.crewvy.workforce_service.reservation.constant.RepeatCycle;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
public class RecurringSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Enumerated(EnumType.STRING)
    private RepeatCycle cycle;

    private int repeatInterval;

    private int dayOfWeek;

    private LocalDate endDate;

    @Builder.Default
    @OneToMany(mappedBy = "recurringSetting", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Reservation> reservation = new ArrayList<>();

}
