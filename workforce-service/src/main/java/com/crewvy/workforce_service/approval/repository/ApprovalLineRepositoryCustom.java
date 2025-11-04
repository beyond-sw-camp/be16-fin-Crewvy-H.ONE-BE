package com.crewvy.workforce_service.approval.repository;

import com.crewvy.workforce_service.approval.constant.LineStatus;
import com.crewvy.workforce_service.approval.entity.ApprovalLine;
import feign.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ApprovalLineRepositoryCustom {
    Page<ApprovalLine> findPendingLinesWithDetails(
            @Param("memberPositionId") UUID memberPositionId,
            @Param("status") LineStatus status,
            Pageable pageable);
}
