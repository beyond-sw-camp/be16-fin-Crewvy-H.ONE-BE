package com.crewvy.workforce_service.attendance.dto.query;

import java.util.UUID;

// N+1 쿼리 해결을 위해 RequestRepository에서 GROUP BY 쿼리 결과를 매핑하기 위한 인터페이스입니다.
public interface PolicyUsageStats {
    UUID getPolicyId();
    Long getCount();
    Double getSum();
}
