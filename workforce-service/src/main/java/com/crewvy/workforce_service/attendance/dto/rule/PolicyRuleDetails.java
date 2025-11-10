package com.crewvy.workforce_service.attendance.dto.rule;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL) // null인 필드는 JSON 변환 시 제외
public class PolicyRuleDetails {
    private WorkTimeRuleDto workTimeRule;
    private AuthRuleDto authRule;
    private GoOutRuleDto goOutRule;
    private BreakRuleDto breakRule;
    private ClockOutRuleDto clockOutRule;
    private LeaveRuleDto leaveRule;
    private TripRuleDto tripRule;
    private ExpenseRuleDto expenseRule;
    private LatenessRuleDto latenessRule;
    private OvertimeRuleDto overtimeRule;
}
