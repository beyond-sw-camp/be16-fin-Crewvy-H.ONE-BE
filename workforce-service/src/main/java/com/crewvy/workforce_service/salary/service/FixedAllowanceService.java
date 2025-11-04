package com.crewvy.workforce_service.salary.service;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.common.exception.PermissionDeniedException;
import com.crewvy.workforce_service.feignClient.MemberClient;
import com.crewvy.workforce_service.feignClient.dto.response.MemberSalaryListRes;
import com.crewvy.workforce_service.salary.dto.request.FixedAllowanceCreateAllReq;
import com.crewvy.workforce_service.salary.dto.request.FixedAllowanceCreateReq;
import com.crewvy.workforce_service.salary.dto.response.FixedAllowanceRes;
import com.crewvy.workforce_service.salary.entity.FixedAllowance;
import com.crewvy.workforce_service.salary.repository.FixedAllowanceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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

    public Long saveFixedAllowance(UUID memberPositionId,
                                   FixedAllowanceCreateReq fixedAllowanceCreateReq) {
        
        // 권한 조회
        ApiResponse<Boolean> hasPermission = memberClient.checkPermission(memberPositionId,
                "salary", "CREATE", "COMPANY");

        if (Boolean.FALSE.equals(hasPermission.getData())) {
            throw new PermissionDeniedException("이 리소스에 접근할 권한이 없습니다.");
        }
        
        return fixedAllowanceRepository.save(fixedAllowanceCreateReq.toEntity()).getId();
    }

    public List<FixedAllowanceRes> getFixedAllowanceList(UUID memberPositionId,
                                                         UUID companyId) {

        // 권한 조회
        ApiResponse<Boolean> hasPermission = memberClient.checkPermission(memberPositionId,
                "salary", "READ", "COMPANY");

        if (Boolean.FALSE.equals(hasPermission.getData())) {
            throw new PermissionDeniedException("이 리소스에 접근할 권한이 없습니다.");
        }

        List<FixedAllowance> fixedAllowanceList = fixedAllowanceRepository.findAllByCompanyId(companyId);
        return fixedAllowanceList.stream().map(FixedAllowanceRes::fromEntity).toList();
    }

    @Transactional
    public void saveAllFixedAllowance(UUID memberPositionId, UUID companyId,
                                      List<FixedAllowanceCreateAllReq> listReq) {

        ApiResponse<Boolean> hasPermission = memberClient.checkPermission(memberPositionId,
                "salary", "CREATE", "COMPANY");

        if (Boolean.FALSE.equals(hasPermission.getData())) {
            throw new PermissionDeniedException("이 리소스에 접근할 권한이 없습니다.");
        }

        ApiResponse<List<MemberSalaryListRes>> salaryListResponse = memberClient.getSalaryList(memberPositionId,
                companyId);
        List<MemberSalaryListRes> memberList = salaryListResponse != null ? salaryListResponse.getData() : new ArrayList<>();

//        List<MemberSalaryListRes> filteredMemberList = memberList.stream()
//                .filter(member -> {
//                    String sabun = member.getSabun();
//                    return sabun != null && !sabun.isEmpty() && Character.isDigit(sabun.charAt(0));
//                }).toList();

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
