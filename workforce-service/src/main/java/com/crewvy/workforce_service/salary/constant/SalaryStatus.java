package com.crewvy.workforce_service.salary.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum SalaryStatus {
    PAID("SS001", "지급 완료"),
    PENDING("SS002", "지급 예정");

    private final String codeValue;
    private final String codeName;

    public static SalaryStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(v -> v.codeValue.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown SalaryStatus code: " + code));
    }
}
