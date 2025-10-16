package com.crewvy.workforce_service.feignClient;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workforce_service.feignClient.dto.request.IdListReq;
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

    // 권한 확인
    @GetMapping("/member/check-permission")
    ApiResponse<Boolean> checkPermission(
            @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
            @RequestParam("resource") String resource,
            @RequestParam("action") String action,
            @RequestParam("range") String range
    );
}
