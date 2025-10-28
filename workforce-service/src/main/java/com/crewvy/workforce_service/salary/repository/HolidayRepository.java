package com.crewvy.workforce_service.salary.repository;

import com.crewvy.workforce_service.salary.entity.Holidays;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface HolidayRepository extends JpaRepository<Holidays, Integer> {
    boolean existsBySolarDate(LocalDate date);
}
