package com.crewvy.workforce_service.salary.service;

import com.crewvy.workforce_service.salary.dto.request.SalaryInfoCreateReq;
import com.crewvy.workforce_service.salary.dto.request.SalaryInfoUpdateReq;
import com.crewvy.workforce_service.salary.dto.response.SalaryInfoRes;
import com.crewvy.workforce_service.salary.entity.PayrollItem;
import com.crewvy.workforce_service.salary.entity.SalaryInfo;
import com.crewvy.workforce_service.salary.repository.PayrollItemRepository;
import com.crewvy.workforce_service.salary.repository.SalaryInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Transactional
@RequiredArgsConstructor
@Service
public class SalaryInfoService {

    private final SalaryInfoRepository salaryInfoRepository;
    private final PayrollItemRepository payrollItemRepository;

    @Transactional(readOnly = true)
    public List<SalaryInfoRes> getSalaryInfoList(UUID companyId) {
        List<SalaryInfo> items = salaryInfoRepository.findByCompanyIdOrderByCreatedAtAsc(companyId);
        return items.stream()
                .map(SalaryInfoRes::from)
                .collect(Collectors.toList());
    }

    public List<SalaryInfoRes> saveSalaryInfoList(List<SalaryInfoCreateReq> requests) {
        List<SalaryInfoRes> salaryInfoResList = new ArrayList<>();
        for (SalaryInfoCreateReq req : requests) {
            PayrollItem payrollItem = payrollItemRepository.findById(req.getPayrollItemId())
                    .orElseThrow(() -> new IllegalArgumentException("급여 항목을 찾을 수 없습니다"));

            SalaryInfo item = req.toEntity(payrollItem);
            SalaryInfo savedItem = salaryInfoRepository.save(item);
            salaryInfoResList.add(SalaryInfoRes.from(savedItem));
        }
        return salaryInfoResList;
    }

    public List<SalaryInfoRes> updateSalaryInfoList(List<SalaryInfoUpdateReq> requests) {
        List<SalaryInfoRes> salaryInfoResList = new ArrayList<>();
        for (SalaryInfoUpdateReq req : requests) {
            SalaryInfo item = salaryInfoRepository.findById(req.getId())
                    .orElseThrow(() -> new IllegalArgumentException("급여 기본 정보를 찾을 수 없습니다."));

            PayrollItem payrollItem = payrollItemRepository.findById(req.getPayrollItemId())
                    .orElseThrow(() -> new IllegalArgumentException("급여 항목을 찾을 수 없습니다."));

            item.updateSalaryInfo(req.getCompanyId(), payrollItem, req.getAmount(), req.getStartDate(), req.getEndDate());
            SalaryInfo savedItem = salaryInfoRepository.save(item);
            salaryInfoResList.add(SalaryInfoRes.from(savedItem));
        }
        return salaryInfoResList;
    }

    public void deleteSalaryInfoList(List<UUID> uuidList) {
        List<SalaryInfo> salaryInfoResList = salaryInfoRepository.findAllById(uuidList);
        if (salaryInfoResList.size() != uuidList.size()) {
            throw new IllegalArgumentException("일부 급여 기본 정보를 찾을 수 없습니다.");
        }
        salaryInfoRepository.deleteAll(salaryInfoResList);
    }
}
