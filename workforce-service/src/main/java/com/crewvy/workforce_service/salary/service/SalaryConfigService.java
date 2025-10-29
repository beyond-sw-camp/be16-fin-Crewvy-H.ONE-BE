package com.crewvy.workforce_service.salary.service;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workforce_service.feignClient.MemberClient;
import com.crewvy.workforce_service.feignClient.dto.response.MemberSalaryListRes;
import com.crewvy.workforce_service.salary.dto.request.SalaryHistoryListReq;
import com.crewvy.workforce_service.salary.dto.response.FixedAllowanceRes;
import com.crewvy.workforce_service.salary.dto.response.SalaryConfigRes;
import com.crewvy.workforce_service.salary.entity.SalaryHistory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SalaryConfigService {

    private final SalaryHistoryService salaryHistoryService;
    private final FixedAllowanceService fixedAllowanceService;
    private final MemberClient memberClient;

    public List<SalaryConfigRes> getSalaryConfigRes(UUID memberPositionId, UUID companyId) {

        ApiResponse<List<MemberSalaryListRes>> salaryListResponse = memberClient.getSalaryList(memberPositionId,
                companyId);
        List<MemberSalaryListRes> memberList = salaryListResponse != null ? salaryListResponse.getData() : new ArrayList<>();

        List<MemberSalaryListRes> filteredMemberList = memberList.stream()
                .filter(member -> {
                    String sabun = member.getSabun();
                    return sabun != null && !sabun.isEmpty() && Character.isDigit(sabun.charAt(0));
                }).toList();

        // 기본급 조회
        YearMonth yearMonth = YearMonth.of(LocalDate.now().getYear(), LocalDate.now().getMonth());
        List<SalaryHistory> salaryHistoryList =  salaryHistoryService
                .getSalaryHistories(new SalaryHistoryListReq(companyId, yearMonth));

        Map<UUID, SalaryHistory> salaryHistoryMap = salaryHistoryList.stream()
                .collect(Collectors.toMap(SalaryHistory::getMemberId
                        , salaryHistory -> salaryHistory));


        // 고정 수당 항목 조회
        List<FixedAllowanceRes> fixedAllowanceResList =  fixedAllowanceService.getFixedAllowanceList(companyId);

        Map<UUID, List<FixedAllowanceRes>> fixedAllowanceResMap = fixedAllowanceResList.stream()
                .collect(Collectors.groupingBy(FixedAllowanceRes::getMemberId));

        return filteredMemberList.stream().map(memberSalaryListRes -> {
            UUID memberId = memberSalaryListRes.getMemberId();

            SalaryHistory salaryHistory = salaryHistoryMap.get(memberId);
            List<FixedAllowanceRes> fixedAllowanceList = fixedAllowanceResMap.getOrDefault(memberId
                                                                                        , Collections.emptyList());

            return SalaryConfigRes.builder()
                    .memberId(memberId)
                    .sabun(memberSalaryListRes.getSabun())
                    .memberName(memberSalaryListRes.getMemberName())
                    .department(memberSalaryListRes.getOrganizationName())
                    .baseSalary(salaryHistory != null ? salaryHistory.getBaseSalary() : 0)
                    .fixedAllowanceList(fixedAllowanceList)
                    .build();
        }).toList();
    }
}
