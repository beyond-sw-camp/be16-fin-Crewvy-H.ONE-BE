package com.crewvy.workforce_service.attendance.entity;

import com.crewvy.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "company_holiday")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyHoliday extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "company_holiday_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID companyHolidayId;

    @Column(name = "company_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID companyId;

    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

    @Column(name = "holiday_name", nullable = false)
    private String holidayName;

    @Column(name = "is_paid", nullable = false)
    private Boolean isPaid;
}
