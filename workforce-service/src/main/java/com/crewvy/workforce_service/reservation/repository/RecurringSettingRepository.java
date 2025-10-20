package com.crewvy.workforce_service.reservation.repository;

import com.crewvy.workforce_service.reservation.entity.RecurringSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurringSettingRepository extends JpaRepository<RecurringSetting, Integer> {
}
