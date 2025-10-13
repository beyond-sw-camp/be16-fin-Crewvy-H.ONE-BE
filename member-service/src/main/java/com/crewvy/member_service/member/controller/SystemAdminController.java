package com.crewvy.member_service.member.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.member_service.member.dto.request.CreatePermissionReq;
import com.crewvy.member_service.member.service.SystemAdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/system-admin")
public class SystemAdminController {
    private final SystemAdminService systemAdminService;

    public SystemAdminController(SystemAdminService systemAdminService) {
        this.systemAdminService = systemAdminService;
    }

    @PostMapping("/create-permission")
    public ResponseEntity<?> createPermission(@RequestHeader("X-User-UUID") UUID uuid,
                                              @RequestBody CreatePermissionReq createPermissionReq) {
        return new ResponseEntity<>(ApiResponse.success(systemAdminService.createPermission(uuid, createPermissionReq),
                "권한 생성 성공"), HttpStatus.CREATED);
    }
}
