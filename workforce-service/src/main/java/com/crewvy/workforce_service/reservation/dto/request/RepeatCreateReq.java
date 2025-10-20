package com.crewvy.workforce_service.reservation.dto.request;

import com.crewvy.workforce_service.reservation.constant.DayOfWeek;
import com.crewvy.workforce_service.reservation.constant.RepeatCycle;
import com.crewvy.workforce_service.reservation.entity.RecurringSetting;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepeatCreateReq {

    private RepeatCycle cycle;
    private int repeatInterval;
    private List<DayOfWeek> dayOfWeek;
    private LocalDate endDate;

    public RecurringSetting toEntity() {

        int dayOfWeekMask = 0;
        if (this.dayOfWeek != null && !this.dayOfWeek.isEmpty()) {
            for (DayOfWeek day : this.dayOfWeek) {
                dayOfWeekMask |= day.getCodeValue();
            }
        }

        return RecurringSetting.builder()
                .cycle(this.cycle)
                .repeatInterval(this.repeatInterval)
                .dayOfWeek(dayOfWeekMask)
                .endDate(this.endDate)
                .build();
    }
}
