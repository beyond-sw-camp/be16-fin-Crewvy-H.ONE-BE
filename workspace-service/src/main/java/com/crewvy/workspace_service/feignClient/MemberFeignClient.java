package com.crewvy.workspace_service.feignClient;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workspace_service.feignClient.dto.IdListReq;
import com.crewvy.workspace_service.feignClient.dto.MemberNameListRes;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "member-service", path = "/member", url = "${feign.client.member-service.url}")
public interface MemberFeignClient {
    @PostMapping("/name-list")
    ApiResponse<List<MemberNameListRes>> getNameList(@RequestHeader("X-User-MemberPositionId") UUID MemberPositionId, @RequestBody IdListReq idListReq);
}
