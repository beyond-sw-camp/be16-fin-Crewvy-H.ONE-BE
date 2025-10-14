package com.crewvy.workforce_service.feignClient;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "member-service")
public interface MemberClient {
}
