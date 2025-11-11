package com.crewvy.workforce_service.salary.service;

import com.crewvy.workforce_service.aop.AuthUser;
import com.crewvy.workforce_service.aop.CheckPermission;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class SalaryHistoryService {

    private final SalaryHistoryRepository salaryHistoryRepository;
    private final SalaryPolicyService salaryPolicyService;

    @Transactional
    @CheckPermission(resource = "salary", action = "CREATE", scope = "COMPANY")
    public void updateSalaryHistory(@AuthUser UUID memberPositionId, UUID companyId,
                                    List<SalaryHistoryCreateReq> reqList) {

        List<UUID> ids = reqList.stream()
                .map(SalaryHistoryCreateReq::getId)
                .toList();

        Map<UUID, SalaryHistory> salaryHistoryMap = salaryHistoryRepository.findAllById(ids)
                .stream()
                .collect(Collectors.toMap(SalaryHistory::getId, entity -> entity));

        List<SalaryHistory> saveList = new ArrayList<>();

        for (SalaryHistoryCreateReq req : reqList) {

            SalaryHistory salaryHistory = salaryHistoryMap.get(req.getId());

            if (salaryHistory != null) {
                salaryHistory.update(req, companyId);
                saveList.add(salaryHistory);
            } else {
                saveList.add(req.toEntity(companyId));
            }
        }

        salaryHistoryRepository.saveAll(saveList);
    }

    @Transactional(readOnly = true)
    public List<SalaryHistoryRes> getSalaryHistoryListByMemberId(UUID memberId) {
        List<SalaryHistory> salaryHistoryList = salaryHistoryRepository.findAllByMemberId(memberId);
        return salaryHistoryList.stream().map(SalaryHistoryRes::fromEntity).toList();
    }

    // 산정 종료일 기반 급여 이력 조회 후 기본급 반환
    public List<SalaryHistory> getSalaryHistories(SalaryHistoryListReq salaryHistoryListReq) {
        SalaryPolicy salaryPolicy
                = salaryPolicyService.getLatestSalaryHistoryForCalculation(salaryHistoryListReq.getCompanyId());
        int payday = salaryPolicy.getPaymentDay();

        LocalDate baseSalaryAsOfDate = (payday == 0) ? salaryHistoryListReq.getYearMonth().atEndOfMonth()
                : salaryHistoryListReq.getYearMonth().atDay(payday);


        log.error("기본급 조회 기준일 (baseSalaryAsOfDate) : {}", baseSalaryAsOfDate);

        return salaryHistoryRepository.findLatestSalaryHistoriesByCompany(
                salaryHistoryListReq.getCompanyId(),
                baseSalaryAsOfDate
        );
    }

}
