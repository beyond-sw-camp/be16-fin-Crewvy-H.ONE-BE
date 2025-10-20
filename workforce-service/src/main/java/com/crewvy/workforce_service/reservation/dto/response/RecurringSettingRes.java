package com.crewvy.workforce_service.reservation.dto.response;

import com.crewvy.workforce_service.reservation.constant.DayOfWeek;
import com.crewvy.workforce_service.reservation.constant.RepeatCycle;
import com.crewvy.workforce_service.reservation.entity.RecurringSetting;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringSettingRes {
    private int id;
    private RepeatCycle cycle;
    private int repeatInterval;
    private LocalDate endDate;
    private List<String> repeatDayList;


    public static RecurringSettingRes fromEntity(RecurringSetting recurringSetting) {

        if (recurringSetting == null) {
            return null;
        }

        int dayOfWeekMask = recurringSetting.getDayOfWeek();

        List<String> selectedDayList = Arrays.stream(DayOfWeek.values())
                .filter(dayOfWeek -> (dayOfWeekMask & dayOfWeek.getCodeValue()) > 0)
                .map(DayOfWeek::name)
                .toList();

        return RecurringSettingRes.builder()
                .id(recurringSetting.getId())
                .cycle(recurringSetting.getCycle())
                .repeatInterval(recurringSetting.getRepeatInterval())
                .endDate(recurringSetting.getEndDate())
                .repeatDayList(selectedDayList)
                .build();
    }
}
