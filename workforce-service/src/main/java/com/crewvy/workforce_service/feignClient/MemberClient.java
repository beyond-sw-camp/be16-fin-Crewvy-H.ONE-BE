package com.crewvy.workforce_service.feignClient;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workforce_service.feignClient.dto.request.IdListReq;
import com.crewvy.workforce_service.feignClient.dto.response.*;
import com.crewvy.workforce_service.feignClient.dto.response.MemberEmploymentInfoDto;
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

    @GetMapping("/member/title")
    ApiResponse<List<TitleRes>>  getTitle(@RequestHeader("X-User-UUID") UUID uuid,
                                          @RequestHeader("X-User-MemberPositionId") UUID memberPositionId);

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
    @GetMapping("/member/{companyId}/salary-list")
    ApiResponse<List<MemberSalaryListRes>> getSalaryList(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                                         @PathVariable UUID companyId);

    // 연차 발생 계산용 회원 고용 정보 조회 (companyId로 조회)
    @GetMapping("/member/employment-info")
    ApiResponse<List<MemberEmploymentInfoDto>> getEmploymentInfo(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                                                  @RequestParam UUID companyId);

    // 내부 전용: 회원 고용 정보 조회 (시스템 배치/내부 작업용 - 권한 체크 없음)
    @GetMapping("/member/internal/company/{companyId}/employment-info")
    ApiResponse<List<MemberEmploymentInfoDto>> getEmploymentInfoInternal(@PathVariable("companyId") UUID companyId);

    // 내부 전용: 단일 회원 고용 정보 조회 (Kafka 이벤트/내부 작업용 - 권한 체크 없음)
    @GetMapping("/member/internal/member/{memberId}/employment-info")
    ApiResponse<MemberEmploymentInfoDto> getMemberEmploymentInfoInternal(@PathVariable("memberId") UUID memberId);

    // 내부 전용: 모든 회사 ID 조회 (테스트 데이터 초기화용 - 권한 체크 없음)
    @GetMapping("/member/internal/all-company-ids")
    ApiResponse<List<UUID>> getAllCompanyIds();
}
