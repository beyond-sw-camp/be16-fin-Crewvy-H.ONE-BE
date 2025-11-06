package com.crewvy.workforce_service.salary.service;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.common.exception.PermissionDeniedException;
import com.crewvy.workforce_service.aop.AuthUser;
import com.crewvy.workforce_service.aop.CheckPermission;
import com.crewvy.workforce_service.feignClient.MemberClient;
import com.crewvy.workforce_service.feignClient.dto.response.MemberSalaryListRes;
import com.crewvy.workforce_service.salary.dto.request.FixedAllowanceCreateAllReq;
import com.crewvy.workforce_service.salary.dto.request.FixedAllowanceCreateReq;
import com.crewvy.workforce_service.salary.dto.response.FixedAllowanceRes;
import com.crewvy.workforce_service.salary.entity.FixedAllowance;
import com.crewvy.workforce_service.salary.repository.FixedAllowanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class FixedAllowanceService {

    private final FixedAllowanceRepository fixedAllowanceRepository;
    private final MemberClient memberClient;

    @CheckPermission(resource = "salary", action = "CREATE", scope = "COMPANY")
    public Long saveFixedAllowance(@AuthUser UUID memberPositionId,
                                   FixedAllowanceCreateReq fixedAllowanceCreateReq) {

        return fixedAllowanceRepository.save(fixedAllowanceCreateReq.toEntity()).getId();
    }

    @Transactional(readOnly = true)
    @CheckPermission(resource = "salary", action = "CREATE", scope = "COMPANY")
    public List<FixedAllowanceRes> getFixedAllowanceList(UUID memberPositionId,
                                                         UUID companyId) {

        List<FixedAllowance> fixedAllowanceList = fixedAllowanceRepository.findAllByCompanyId(companyId);
        return fixedAllowanceList.stream().map(FixedAllowanceRes::fromEntity).toList();
    }

    @Transactional
    @CheckPermission(resource = "salary", action = "CREATE", scope = "COMPANY")
    public void saveAllFixedAllowance(UUID memberPositionId, UUID companyId,
                                      List<FixedAllowanceCreateAllReq> listReq) {

        ApiResponse<List<MemberSalaryListRes>> salaryListResponse = memberClient.getSalaryList(memberPositionId,
                companyId);
        List<MemberSalaryListRes> memberList = salaryListResponse != null ? salaryListResponse.getData() : new ArrayList<>();

        for (MemberSalaryListRes memberSalaryListRes : memberList) {
            UUID memberId = memberSalaryListRes.getMemberId();

            for (FixedAllowanceCreateAllReq req : listReq) {
                Optional<FixedAllowance> fixedAllowance = fixedAllowanceRepository
                        .findByMemberIdAndAllowanceName(memberId, req.getAllowanceName());

                if (fixedAllowance.isPresent()) {
                    FixedAllowance update = fixedAllowance.get();
                    update.updateAmount(req.getAmount());
                    update.updateEffectiveDate(req.getEffectiveDate());
                } else {
                    fixedAllowanceRepository.save(req.toEntity(memberId, companyId));
                }

            }
        }
    }
}
