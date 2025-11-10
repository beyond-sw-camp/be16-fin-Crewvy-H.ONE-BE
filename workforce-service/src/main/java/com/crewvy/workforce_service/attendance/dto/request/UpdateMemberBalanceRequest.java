package com.crewvy.workforce_service.attendance.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMemberBalanceRequest {

    @NotNull(message = "총 부여일수는 필수입니다.")
    @PositiveOrZero(message = "총 부여일수는 0 이상이어야 합니다.")
    private Double totalGranted;

    @NotNull(message = "총 사용일수는 필수입니다.")
    @PositiveOrZero(message = "총 사용일수는 0 이상이어야 합니다.")
    private Double totalUsed;
}
