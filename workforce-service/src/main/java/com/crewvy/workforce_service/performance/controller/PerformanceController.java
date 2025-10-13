package com.crewvy.workforce_service.performance.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workforce_service.performance.dto.*;
import com.crewvy.workforce_service.performance.service.PerformanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/performance")
public class PerformanceController {
    private final PerformanceService performanceService;

    @GetMapping("/team-goal")
    public ResponseEntity<?> getTeamGoal() {
        List<TeamGoalResponseDto> teamGoalResponseDtoList = performanceService.getTeamGoal();
        return new ResponseEntity<>(
                ApiResponse.success(teamGoalResponseDtoList, "팀 목표 조회"),
                HttpStatus.OK
        );
    }

    @GetMapping("team-goal/{id}")
    public ResponseEntity<?> getSubGoal(@PathVariable UUID id) {
        TeamGoalDetailResponseDto dto = performanceService.getSubGoal(id);
        return new ResponseEntity<>(
                ApiResponse.success(dto, "팀 목표 상세 조회"),
                HttpStatus.OK
        );
    }

    @PostMapping("/create-team-goal")
    public ResponseEntity<?> createTeamGoal(@RequestBody CreateTeamGoalDto dto) {
        UUID newTeamGoalId = performanceService.createTeamGoal(dto);
        return new ResponseEntity<>(
                ApiResponse.success(newTeamGoalId, "팀 목표 생성"),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/get-goal-detail/{id}")
    public ResponseEntity<?> getGoalDetail(@PathVariable UUID id) {
        GoalResponseDto dto = performanceService.getGoalDetail(id);
        return new ResponseEntity<>(
                ApiResponse.success(dto, "목표 상세 조회"),
                HttpStatus.OK
        );
    }

    @GetMapping("/get-my-goal")
    public ResponseEntity<?> getMyGoal() {
        List<GoalResponseDto> dtoList = performanceService.getMyGoal();
        return new ResponseEntity<>(
                ApiResponse.success(dtoList, "내 목표 조회"),
                HttpStatus.OK
        );
    }

    @PostMapping("/create-my-goal")
    public ResponseEntity<?> createMyGoal(@RequestBody CreateMyGoalDto dto) {
        UUID goalId = performanceService.createMyGoal(dto);
        return new ResponseEntity<>(
                ApiResponse.success(goalId, "내 목표 생성"),
                HttpStatus.CREATED
        );
    }

    @PatchMapping("/update-status")
    public ResponseEntity<?> updateGoalStatus(UpdateStatusDto dto) {
        UUID goalId = performanceService.updateGoalStatus(dto);
        return new ResponseEntity<>(
                ApiResponse.success(goalId,"목표 상태 변경"),
                HttpStatus.OK
        );
    }

    @PostMapping("/create-evaluation")
    public ResponseEntity<?> createEvaluation(CreateEvaluationDto dto) {
        UUID evaluationId = performanceService.createEvaluation(dto);
        return new ResponseEntity<>(
                ApiResponse.success(evaluationId, "평가 생성"),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/find-evaluation")
    public ResponseEntity<?> findEvaluation(FindEvaluationDto dto) {
        EvaluationResponseDto responseDto = performanceService.findEvaluation(dto);
        return new ResponseEntity<>(
                ApiResponse.success(responseDto, "평가 조회"),
                HttpStatus.OK
        );
    }

    @PatchMapping("/update-my-goal")
    public ResponseEntity<?> updateMyGoal(@RequestBody UpdateMyGoalDto dto) {
        UUID goalId = performanceService.updateMyGoal(dto);
        return new ResponseEntity<>(
                ApiResponse.success(goalId, "목표 수정"),
                HttpStatus.OK
        );
    }

    @PatchMapping("/evidence/{id}")
    public ResponseEntity<?> patchEvidence(@PathVariable UUID id,
                                           @RequestPart("evidenceInfo") EvidenceRequestDto dto,
                                           @RequestPart(value = "newFiles", required = false) List<MultipartFile> newFiles)
    {
        performanceService.patchEvidence(id, dto, newFiles);
        return new ResponseEntity<>(
                ApiResponse.success(id,"증적 제출 및 수정"),
                HttpStatus.OK
        );
    }
}
