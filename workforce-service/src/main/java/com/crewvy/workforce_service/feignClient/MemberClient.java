package com.crewvy.workforce_service.feignClient;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "member-client", url = "${feign.client.member-service.url}")
public interface MemberClient {

}
