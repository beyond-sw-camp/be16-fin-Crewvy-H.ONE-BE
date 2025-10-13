package com.crewvy.workforce_service.approval.dto.response;

import com.crewvy.workforce_service.approval.constant.LineStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ApprovalStepDto {
    private String approverName;
    private String approverGrade;
    private LineStatus status;
    private int index;
}
