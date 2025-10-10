package com.crewvy.workforce_service.reservation.entity;

import com.crewvy.common.entity.BaseEntity;
import com.crewvy.workforce_service.reservation.dto.request.ReservationCategoryUpdateReq;
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
public class ReservationCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private UUID companyId;


    public void update(ReservationCategoryUpdateReq req) {
        if (req.getName() != null) {
            this.name = req.getName();
        }
    }

}
