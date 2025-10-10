package com.crewvy.workforce_service.performance.controller;

import com.crewvy.workforce_service.performance.dto.*;
import com.crewvy.workforce_service.performance.service.PerformanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        return new ResponseEntity<>(teamGoalResponseDtoList, HttpStatus.OK);
    }

    @GetMapping("team-goal/{id}")
    public ResponseEntity<?> getSubGoal(@PathVariable UUID id) {
        TeamGoalDetailResponseDto dto = performanceService.getSubGoal(id);
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    @PostMapping("/create-team-goal")
    public ResponseEntity<?> createTeamGoal(@RequestBody CreateTeamGoalDto dto) {
        performanceService.createTeamGoal(dto);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping("/get-goal-detail/{id}")
    public ResponseEntity<?> getGoalDetail(@PathVariable UUID id) {
        GoalResponseDto dto = performanceService.getGoalDetail(id);
        return new ResponseEntity<>(dto, HttpStatus.OK);
    }

    @GetMapping("/get-my-goal")
    public ResponseEntity<?> getMyGoal() {
        List<GoalResponseDto> dtoList = performanceService.getMyGoal();
        return new ResponseEntity<>(dtoList, HttpStatus.OK);
    }

    @PostMapping("/create-my-goal")
    public ResponseEntity<?> createMyGoal(@RequestBody CreateMyGoalDto dto) {
        performanceService.createMyGoal(dto);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PatchMapping("/update-status")
    public ResponseEntity<?> updateGoalStatus(UpdateStatusDto dto) {
        performanceService.updateGoalStatus(dto);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
