package com.crewvy.workforce_service.salary.entity;

import com.crewvy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
public class FixedAllowance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private UUID memberId;

    @Column(nullable = false)
    private String allowanceName;

    @Column(nullable = false)
    private int amount;

    @Column(nullable = false)
    private LocalDate effectiveDate;
}
