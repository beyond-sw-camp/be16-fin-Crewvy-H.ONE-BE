package com.crewvy.member_service.member.service;

import com.crewvy.member_service.member.constant.Action;
import com.crewvy.member_service.member.constant.PermissionRange;
import com.crewvy.member_service.member.dto.request.CreatePermissionReq;
import com.crewvy.member_service.member.repository.MemberRepository;
import com.crewvy.member_service.member.repository.PermissionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class SystemAdminService {
    private final PermissionRepository permissionRepository;

    public SystemAdminService(PermissionRepository permissionRepository) {
        this.permissionRepository = permissionRepository;
    }

    public UUID createPermission(UUID uuid, CreatePermissionReq createPermissionReq){
        if (permissionRepository.findByResourceAndActionAndPermissionRange(createPermissionReq.getResource(),
                Action.fromCode(createPermissionReq.getAction()),
                PermissionRange.fromCode(createPermissionReq.getPermissionRange())).isPresent()){
            throw new DataIntegrityViolationException("이미 존재하는 권한입니다.");
        }

        return permissionRepository.save(createPermissionReq.toEntity()).getId();
    }

//    public List<Permission> getPermission(String email){
//
//    }
}
