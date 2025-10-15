package com.crewvy.workforce_service.feignClient;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workforce_service.feignClient.dto.response.PositionDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "member-service", url = "${feign.client.member-service.url}")
public interface MemberClient {
    @GetMapping("/member/position-list")
    ApiResponse<List<PositionDto>> getPositionList( // 반환 타입은 실제 DTO에 맞게 지정
                                                    @RequestHeader("X-User-MemberPositionId") UUID memberPositionId,
                                                    @RequestParam List<UUID> idListReq
    );
}
