package com.crewvy.workforce_service.performance.controller;

import com.crewvy.workforce_service.performance.dto.CreateTeamGoalDto;
import com.crewvy.workforce_service.performance.dto.TeamGoalDetailResponseDto;
import com.crewvy.workforce_service.performance.dto.TeamGoalResponseDto;
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
}
