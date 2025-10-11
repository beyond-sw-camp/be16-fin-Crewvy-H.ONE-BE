package com.crewvy.workforce_service.performance.service;

import com.crewvy.common.S3.S3Uploader;
import com.crewvy.workforce_service.performance.constant.GoalStatus;
import com.crewvy.workforce_service.performance.dto.*;
import com.crewvy.workforce_service.performance.entity.Evaluation;
import com.crewvy.workforce_service.performance.entity.Evidence;
import com.crewvy.workforce_service.performance.entity.Goal;
import com.crewvy.workforce_service.performance.entity.TeamGoal;
import com.crewvy.workforce_service.performance.repository.EvaluationRepository;
import com.crewvy.workforce_service.performance.repository.PerformanceRepository;
import com.crewvy.workforce_service.performance.repository.TeamGoalRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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

    //    하위목표 상세조회
    public GoalResponseDto getGoalDetail(UUID id) {
        Goal goal = performanceRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 목표입니다."));

//        증적 조회
        List<EvidenceResponseDto> evidenceList = new ArrayList<>();
        for(Evidence e : goal.getEvidenceList()) {
            EvidenceResponseDto dto = EvidenceResponseDto.builder()
                    .evidenceId(e.getId())
                    .evidenceUrl(e.getEvidenceUrl())
                    .build();
            evidenceList.add(dto);
        }

        return GoalResponseDto.builder()
                .goalId(goal.getId())
                .title(goal.getTitle())
                .contents(goal.getContents())
                .startDate(goal.getStartDate())
                .endDate(goal.getEndDate())
                .status(goal.getStatus())
                .teamGoalTitle(goal.getTeamGoal().getTitle())
                .teamGoalContents(goal.getTeamGoal().getContents())
                .gradingSystem(goal.getGradingSystem())
                .evidenceList(evidenceList)
                .build();
    }

    //    내 목표 조회
    public List<GoalResponseDto> getMyGoal() {
//        List<Goal> goalList = performanceRepository.findByMemberId();
        List<Goal> goalList = performanceRepository.findAll();
        List<GoalResponseDto> dtoList = new ArrayList<>();
        for(Goal g : goalList) {
            GoalResponseDto dto = GoalResponseDto.builder()
                    .goalId(g.getId())
                    .title(g.getTitle())
                    .contents(g.getContents())
                    .status(g.getStatus())
                    .startDate(g.getStartDate())
                    .endDate(g.getEndDate())
                    .build();
            dtoList.add(dto);
        }

        return dtoList;
    }

    //    내 목표 생성
    public void createMyGoal(CreateMyGoalDto dto) {
        TeamGoal teamGoal = teamGoalRepository.findById(dto.getTeamGoalId()).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 팀목표 입니다."));

        Goal newGoal = Goal.builder()
                .title(dto.getTitle())
                .contents(dto.getContents())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .status(GoalStatus.REQUESTED)
                .teamGoal(teamGoal)
                .gradingSystem(dto.getGradingSystem())
                .build();

        performanceRepository.save(newGoal);
    }

    //    목표 상태변경(승인, 반려 등)
    public void updateGoalStatus(UpdateStatusDto dto) {
        Goal goal = performanceRepository.findById(dto.getGoalId()).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 목표입니다."));

        goal.updateStatus(dto.getStatus(), dto.getComment());
    }

    //    평가 생성
    public void createEvaluation(CreateEvaluationDto dto) {
        Goal goal = performanceRepository.findById(dto.getGoalId()).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 목표입니다."));

        Evaluation evaluation = Evaluation.builder()
                .goal(goal)
                .grade(dto.getGrade())
                .type(dto.getType())
                .comment(dto.getComment())
                .build();

        evaluationRepository.save(evaluation);
    }

    //    평가 조회
    public EvaluationResponseDto findEvaluation(FindEvaluationDto dto) {
        Goal goal = performanceRepository.findById(dto.getGoalId()).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 목표입니다."));
        Evaluation evaluation = evaluationRepository.findByGoalAndType(goal, dto.getType()).orElseThrow(() -> new EntityNotFoundException("평가가 존재하지 않습니다."));
        return EvaluationResponseDto.builder()
                .evaluationId(evaluation.getId())
                .grade(evaluation.getGrade())
                .comment(evaluation.getComment())
                .build();
    }

    //    내 목표 업데이트
    public void updateMyGoal(UpdateMyGoalDto dto) {
        Goal goal = performanceRepository.findById(dto.getGoalId()).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 목표입니다."));
        goal.updateGoal(dto);
    }

    //    증적 제출 및 수정
    public void patchEvidence(UUID id, EvidenceRequestDto dto, List<MultipartFile> newFiles) {
        Goal goal = performanceRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 목표입니다."));

        processDeletions(goal, dto.getExistingFileIds());

        processAdditions(goal, newFiles);
    }

    private void processDeletions(Goal goal, List<UUID> idsToKeep) {
        List<Evidence> currentFiles = goal.getEvidenceList();

        List<Evidence> filesToDelete = currentFiles.stream()
                .filter(file -> !idsToKeep.contains(file.getId()))
                .toList();

        if(!filesToDelete.isEmpty()) {
            filesToDelete.forEach(file -> s3Uploader.delete(file.getEvidenceUrl()));

            currentFiles.removeAll(filesToDelete);
        }
    }

    private void processAdditions(Goal goal, List<MultipartFile> newFiles) {
        if(newFiles != null && !newFiles.isEmpty()) {
            newFiles.forEach(file -> {
                String uploadUrl = s3Uploader.upload(file, "evidence");
                Evidence newEvidence = Evidence.builder()
                        .evidenceUrl(uploadUrl)
                        .goal(goal)
                        .build();

                goal.getEvidenceList().add(newEvidence);
            });
        }
    }

//    member쪽 데이터가 전혀 들어가지 않았기에 추후 수정 예정
//    평가쪽 API의 경우 추가적으로 수정 예정
}
