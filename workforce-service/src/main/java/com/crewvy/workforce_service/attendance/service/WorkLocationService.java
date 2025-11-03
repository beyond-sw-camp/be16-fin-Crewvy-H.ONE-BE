package com.crewvy.workforce_service.attendance.service;

import com.crewvy.common.exception.BusinessException;
import com.crewvy.common.exception.ResourceNotFoundException;
import com.crewvy.workforce_service.attendance.dto.request.WorkLocationCreateDto;
import com.crewvy.workforce_service.attendance.dto.request.WorkLocationUpdateDto;
import com.crewvy.workforce_service.attendance.dto.response.WorkLocationResponse;
import com.crewvy.workforce_service.attendance.entity.WorkLocation;
import com.crewvy.workforce_service.attendance.repository.WorkLocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class WorkLocationService {

    private final WorkLocationRepository workLocationRepository;

    /**
     * 근무지 생성
     */
    public WorkLocationResponse createWorkLocation(UUID companyId, WorkLocationCreateDto dto) {
        // 1. 중복 체크 (같은 회사 내에서 근무지 명칭 중복 불가)
        boolean exists = workLocationRepository.existsByCompanyIdAndName(companyId, dto.getName(), null);
        if (exists) {
            throw new BusinessException("이미 같은 이름의 근무지가 존재합니다: " + dto.getName());
        }

        // 2. WorkLocation 엔티티 생성
        WorkLocation workLocation = WorkLocation.builder()
                .companyId(companyId)
                .name(dto.getName())
                .address(dto.getAddress())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .gpsRadius(dto.getGpsRadius())
                .ipAddress(dto.getIpAddress())
                .wifiSsid(dto.getWifiSsid())
                .wifiBssid(dto.getWifiBssid())
                .isActive(dto.getIsActive())
                .description(dto.getDescription())
                .build();

        WorkLocation saved = workLocationRepository.save(workLocation);
        log.info("근무지 생성 완료: {} ({})", saved.getName(), saved.getId());

        return WorkLocationResponse.from(saved);
    }

    /**
     * 근무지 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<WorkLocationResponse> getWorkLocations(UUID companyId, Pageable pageable) {
        Page<WorkLocation> workLocations = workLocationRepository.findByCompanyId(companyId, pageable);
        return workLocations.map(WorkLocationResponse::from);
    }

    /**
     * 활성화된 근무지 목록 조회 (전체)
     */
    @Transactional(readOnly = true)
    public List<WorkLocationResponse> getActiveWorkLocations(UUID companyId) {
        List<WorkLocation> workLocations = workLocationRepository.findByCompanyIdAndIsActive(companyId, true);
        return workLocations.stream()
                .map(WorkLocationResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 근무지 상세 조회
     */
    @Transactional(readOnly = true)
    public WorkLocationResponse getWorkLocationById(UUID workLocationId, UUID companyId) {
        WorkLocation workLocation = workLocationRepository.findByIdAndCompanyId(workLocationId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("근무지를 찾을 수 없습니다."));

        return WorkLocationResponse.from(workLocation);
    }

    /**
     * 근무지 정보 수정
     */
    public WorkLocationResponse updateWorkLocation(UUID workLocationId, UUID companyId, WorkLocationUpdateDto dto) {
        // 1. 근무지 조회
        WorkLocation workLocation = workLocationRepository.findByIdAndCompanyId(workLocationId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("근무지를 찾을 수 없습니다."));

        // 2. 명칭 중복 체크 (자신 제외)
        boolean exists = workLocationRepository.existsByCompanyIdAndName(companyId, dto.getName(), workLocationId);
        if (exists) {
            throw new BusinessException("이미 같은 이름의 근무지가 존재합니다: " + dto.getName());
        }

        // 3. 정보 업데이트
        workLocation.updateInfo(
                dto.getName(),
                dto.getAddress(),
                dto.getLatitude(),
                dto.getLongitude(),
                dto.getGpsRadius(),
                dto.getIpAddress(),
                dto.getWifiSsid(),
                dto.getWifiBssid(),
                dto.getDescription()
        );

        log.info("근무지 수정 완료: {} ({})", workLocation.getName(), workLocation.getId());

        return WorkLocationResponse.from(workLocation);
    }

    /**
     * 근무지 활성/비활성 상태 변경
     */
    public WorkLocationResponse toggleActiveStatus(UUID workLocationId, UUID companyId) {
        WorkLocation workLocation = workLocationRepository.findByIdAndCompanyId(workLocationId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("근무지를 찾을 수 없습니다."));

        workLocation.updateActiveStatus(!workLocation.getIsActive());

        log.info("근무지 상태 변경: {} -> {} ({})",
                workLocation.getName(),
                workLocation.getIsActive() ? "활성" : "비활성",
                workLocation.getId());

        return WorkLocationResponse.from(workLocation);
    }

    /**
     * 근무지 삭제
     */
    public void deleteWorkLocation(UUID workLocationId, UUID companyId) {
        WorkLocation workLocation = workLocationRepository.findByIdAndCompanyId(workLocationId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("근무지를 찾을 수 없습니다."));

        // TODO: 삭제 전 사용 중인지 확인 (Policy, Request 등에서 참조 중인지)
        // 실제로는 soft delete 또는 비활성화 권장

        workLocationRepository.delete(workLocation);

        log.info("근무지 삭제 완료: {} ({})", workLocation.getName(), workLocation.getId());
    }
}
