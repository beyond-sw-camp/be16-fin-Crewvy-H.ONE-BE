package com.crewvy.workforce_service.salary.service;

import com.crewvy.workforce_service.salary.dto.request.SalaryHistoryCreateReq;
import com.crewvy.workforce_service.salary.dto.request.SalaryHistoryListReq;
import com.crewvy.workforce_service.salary.dto.response.SalaryHistoryRes;
import com.crewvy.workforce_service.salary.entity.SalaryHistory;
import com.crewvy.workforce_service.salary.entity.SalaryPolicy;
import com.crewvy.workforce_service.salary.repository.SalaryHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;


@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class SalaryHistoryService {

    private final SalaryHistoryRepository salaryHistoryRepository;
    private final SalaryPolicyService salaryPolicyService;

    public SalaryHistoryRes insertSalaryHistory(SalaryHistoryCreateReq req) {
        SalaryHistory saved = salaryHistoryRepository.save(req.toEntity());
        return SalaryHistoryRes.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<SalaryHistoryRes> getSalaryHistoryListByMemberId(UUID memberId) {
        List<SalaryHistory> salaryHistoryList = salaryHistoryRepository.findAllByMemberId(memberId);
        return salaryHistoryList.stream().map(SalaryHistoryRes::fromEntity).toList();
    }

    // 산정 종료일 기반 급여 이력 조회 후 기본급 반환
    public List<SalaryHistory> getSalaryHistories(SalaryHistoryListReq salaryHistoryListReq) {
        SalaryPolicy salaryPolicy = salaryPolicyService.getLatestSalaryHistoryForCalculation(salaryHistoryListReq.getCompanyId());
        LocalDate endDate = salaryPolicyService.calculatePeriodEndDate(salaryPolicy, salaryHistoryListReq.getYearMonth()).getEndDate();
        return salaryHistoryRepository.findLatestSalaryHistoriesByCompany(salaryHistoryListReq.getCompanyId(), endDate);
    }

}
