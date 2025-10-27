package com.crewvy.workforce_service.performance.controller;

import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workforce_service.performance.dto.request.*;
import com.crewvy.workforce_service.performance.dto.response.EvaluationResponseDto;
import com.crewvy.workforce_service.performance.dto.response.GoalResponseDto;
import com.crewvy.workforce_service.performance.dto.response.TeamGoalDetailResponseDto;
import com.crewvy.workforce_service.performance.dto.response.TeamGoalResponseDto;
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
    public ResponseEntity<?> getTeamGoal(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId) {
        List<TeamGoalResponseDto> teamGoalResponseDtoList = performanceService.getTeamGoal(memberPositionId);
        return new ResponseEntity<>(
                ApiResponse.success(teamGoalResponseDtoList, "팀 목표 조회"),
                HttpStatus.OK
        );
    }

    @GetMapping("/team-goal-processing")
    public ResponseEntity<?> getTeamGoalProcessing(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId) {
        List<TeamGoalResponseDto> teamGoalResponseDtoList = performanceService.getTeamGoalProcessing(memberPositionId);
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
    public ResponseEntity<?> createTeamGoal(@RequestBody CreateTeamGoalDto dto,
                                            @RequestHeader("X-User-MemberPositionId") UUID memberPositionId
    ) {
        UUID newTeamGoalId = performanceService.createTeamGoal(dto, memberPositionId);
        return new ResponseEntity<>(
                ApiResponse.success(newTeamGoalId, "팀 목표 생성"),
                HttpStatus.CREATED
        );
    }

    @PatchMapping("/update-team-goal/{id}")
    public ResponseEntity<?> updateTeamGoal(@PathVariable UUID id,
                                            @RequestBody CreateTeamGoalDto dto,
                                            @RequestHeader("X-User-MemberPositionId") UUID memberPositionId
    ) {
        performanceService.updateTeamGoal(id, dto, memberPositionId);
        return new ResponseEntity<>(
                ApiResponse.success(id, "팀 목표 업데이트"),
                HttpStatus.OK
        );
    }

    @DeleteMapping("/delete-team-goal/{id}")
    public ResponseEntity<?> deleteTeamGoal(@PathVariable UUID id,
                                            @RequestHeader("X-User-MemberPositionId") UUID memberPositionId
    ) {
        performanceService.deleteTeamGoal(id, memberPositionId);
        return new ResponseEntity<>(
                ApiResponse.success(id, "팀 목표 삭제"),
                HttpStatus.OK
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
    public ResponseEntity<?> getMyGoal(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId) {
        List<GoalResponseDto> dtoList = performanceService.getMyGoal(memberPositionId);
        return new ResponseEntity<>(
                ApiResponse.success(dtoList, "내 목표 조회"),
                HttpStatus.OK
        );
    }

    @PostMapping("/create-my-goal")
    public ResponseEntity<?> createMyGoal(@RequestBody CreateMyGoalDto dto,
                                          @RequestHeader("X-User-MemberPositionId") UUID memberPositionId
    ) {
        UUID goalId = performanceService.createMyGoal(dto, memberPositionId);
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
    public ResponseEntity<?> createEvaluation(@RequestBody CreateEvaluationDto dto,
                                              @RequestHeader("X-User-MemberPositionId") UUID memberPositionId
    ) {
        UUID evaluationId = performanceService.createEvaluation(dto, memberPositionId);
        return new ResponseEntity<>(
                ApiResponse.success(evaluationId, "평가 생성"),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/find-evaluation/{goalId}")
    public ResponseEntity<?> findEvaluation(@PathVariable UUID goalId) {
        return new ResponseEntity<>(
                ApiResponse.success(performanceService.findEvaluation(goalId), "평가 조회"),
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

    @GetMapping("/find-teamgoal-evaluation")
    public ResponseEntity<?> findMyTeamGoalToEvaluation(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId) {
        return new ResponseEntity<>(
                ApiResponse.success(performanceService.findMyTeamGoalsToEvaluate(memberPositionId), "평가대상 팀 목표 조회"),
                HttpStatus.OK
        );
    }

    @GetMapping("/find-goal-evaluation")
    public ResponseEntity<?> findMyGoalToEvaluation(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId) {
        return new ResponseEntity<>(
                ApiResponse.success(performanceService.findMyGoalToEvaluate(memberPositionId), "평가대상 개인 목표 조회"),
                HttpStatus.OK
        );
    }

    @GetMapping("/find-complete-teamgoal")
    public ResponseEntity<?> findTeamGoalComplete(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId) {
        return new ResponseEntity<>(
                ApiResponse.success(performanceService.findMyTeamGoalsComplete(memberPositionId), "평가완료상태 팀목표 조회"),
                HttpStatus.OK
        );
    }

    @GetMapping("/find-complete-goal")
    public ResponseEntity<?> findMyGoalComplete(@RequestHeader("X-User-MemberPositionId") UUID memberPositionId) {
        return new ResponseEntity<>(
                ApiResponse.success(performanceService.findMyGoalComplete(memberPositionId), "평가완료상태 팀목표 조회"),
                HttpStatus.OK
        );
    }
}
