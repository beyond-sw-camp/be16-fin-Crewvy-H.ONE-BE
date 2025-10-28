package com.crewvy.workforce_service.feignClient;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workforce_service.feignClient.dto.request.IdListReq;
import com.crewvy.workforce_service.feignClient.dto.response.*;
import com.crewvy.workforce_service.feignClient.dto.response.MemberSalaryListRes;
import com.crewvy.workforce_service.feignClient.dto.response.NameDto;
import com.crewvy.workforce_service.feignClient.dto.response.OrganizationNodeDto;
import com.crewvy.workforce_service.feignClient.dto.response.PositionDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "member-service", url = "${feign.client.member-service.url}")
public interface MemberClient {
    @PostMapping("/member/position-list")
    ApiResponse<List<PositionDto>> getPositionList( // 반환 타입은 실제 DTO에 맞게 지정
                                                    @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                                    @RequestBody IdListReq idListReq
    );
    @PostMapping("/member/default-position-list")
    ApiResponse<List<MemberPositionListRes>> getDefaultPositionList(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                                              @RequestBody IdListReq idListReq);

    @GetMapping("/organization/tree-with-members")
    ApiResponse<List<OrganizationNodeDto>> getOrganization(@RequestHeader("X-User-UUID") UUID uuid);

    // 권한 확인
    @GetMapping("/member/check-permission")
    ApiResponse<Boolean> checkPermission(
            @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
            @RequestParam("resource") String resource,
            @RequestParam("action") String action,
            @RequestParam("range") String range
    );

    @PostMapping("/member/name-list")
    ApiResponse<List<NameDto>> getNameList(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                           @RequestBody IdListReq idListReq);

    // memberPositionId -> 조직 List( 0: 내 부서, 1: 상위 부서, 2: 1의 상위 부서, ... , n: 회사 )
    @GetMapping("/member/organization-list")
    ApiResponse<List<OrganizationRes>> getOrganizationList(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId);

    // 급여 계산용 회원 정보 조회 (companyId로 조회)
    @GetMapping("/member/salary-list")
    ApiResponse<List<MemberSalaryListRes>> getSalaryList(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                                         @RequestParam UUID companyId);
}
