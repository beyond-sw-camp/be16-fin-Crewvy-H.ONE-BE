package com.crewvy.workforce_service.performance.service;

import com.crewvy.common.S3.S3Uploader;
import com.crewvy.workforce_service.performance.dto.CreateTeamGoalDto;
import com.crewvy.workforce_service.performance.dto.GoalResponseDto;
import com.crewvy.workforce_service.performance.dto.TeamGoalDetailResponseDto;
import com.crewvy.workforce_service.performance.dto.TeamGoalResponseDto;
import com.crewvy.workforce_service.performance.entity.Goal;
import com.crewvy.workforce_service.performance.entity.TeamGoal;
import com.crewvy.workforce_service.performance.repository.EvaluationRepository;
import com.crewvy.workforce_service.performance.repository.PerformanceRepository;
import com.crewvy.workforce_service.performance.repository.TeamGoalRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class PerformanceService {
    private final PerformanceRepository performanceRepository;
    private final TeamGoalRepository teamGoalRepository;
    private final EvaluationRepository evaluationRepository;
    private final S3Uploader s3Uploader;

//    팀 목표 리스트
    public List<TeamGoalResponseDto> getTeamGoal() {
        List<TeamGoal> teamGoalList = teamGoalRepository.findAll();
        List<TeamGoalResponseDto> dtoList = new ArrayList<>();
        for(TeamGoal t : teamGoalList) {
            TeamGoalResponseDto dto = TeamGoalResponseDto.builder()
                    .teamGoalId(t.getId())
                    .title(t.getTitle())
                    .contents(t.getContents())
                    .startDate(t.getStartDate())
                    .endDate(t.getEndDate())
                    .build();
            dtoList.add(dto);
        }
        return dtoList;
    }

    //    팀 목표 하위 목표 리스트
    public TeamGoalDetailResponseDto getSubGoal(UUID teamGoalId) {
        TeamGoal teamGoal = teamGoalRepository.findById(teamGoalId).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 팀목표 입니다."));
        List<Goal> subGoalList = performanceRepository.findByTeamGoal(teamGoal);
        TeamGoalDetailResponseDto dto = TeamGoalDetailResponseDto.builder()
                .title(teamGoal.getTitle())
                .contents(teamGoal.getContents())
                .startDate(teamGoal.getStartDate())
                .endDate(teamGoal.getEndDate())
                .build();

        for(Goal g : subGoalList) {
            GoalResponseDto goalDto = GoalResponseDto.builder()
                    .goalId(g.getId())
                    .title(g.getTitle())
                    .contents(g.getContents())
                    .startDate(g.getStartDate())
                    .endDate(g.getEndDate())
                    .status(g.getStatus())
                    .build();
            dto.getGoalList().add(goalDto);
        }
        return dto;
    }

    //    팀 목표 생성
    public void createTeamGoal(CreateTeamGoalDto dto) {
        TeamGoal newTeamGoal = TeamGoal.builder()
                .title(dto.getTitle())
                .contents(dto.getContents())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .build();
        teamGoalRepository.save(newTeamGoal);
    }
}
