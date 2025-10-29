package com.crewvy.workforce_service.salary.service;

import com.crewvy.workforce_service.salary.dto.request.FixedAllowanceCreateReq;
import com.crewvy.workforce_service.salary.dto.response.FixedAllowanceRes;
import com.crewvy.workforce_service.salary.entity.FixedAllowance;
import com.crewvy.workforce_service.salary.repository.FixedAllowanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class FixedAllowanceService {

    private final FixedAllowanceRepository fixedAllowanceRepository;

    public Long saveFixedAllowance(FixedAllowanceCreateReq fixedAllowanceCreateReq) {
        return fixedAllowanceRepository.save(fixedAllowanceCreateReq.toEntity()).getId();
    }

    public List<FixedAllowanceRes> getFixedAllowanceList(UUID companyId) {
        List<FixedAllowance> fixedAllowanceList = fixedAllowanceRepository.findAllByCompanyId(companyId);
        return fixedAllowanceList.stream().map(FixedAllowanceRes::fromEntity).toList();
    }
}
