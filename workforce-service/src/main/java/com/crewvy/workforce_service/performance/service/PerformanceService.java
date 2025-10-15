package com.crewvy.workforce_service.performance.service;

import com.crewvy.common.S3.S3Uploader;
import com.crewvy.common.dto.ApiResponse;
import com.crewvy.workforce_service.feignClient.MemberClient;
import com.crewvy.workforce_service.feignClient.dto.request.IdListReq;
import com.crewvy.workforce_service.feignClient.dto.response.PositionDto;
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
import jakarta.persistence.Id;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class PerformanceService {
    private final PerformanceRepository performanceRepository;
    private final TeamGoalRepository teamGoalRepository;
    private final EvaluationRepository evaluationRepository;
    private final S3Uploader s3Uploader;
    private final MemberClient memberClient;

//    팀 목표 리스트
    public List<TeamGoalResponseDto> getTeamGoal(UUID memberPositionId) {
        List<TeamGoal> teamGoalList = teamGoalRepository.findAll();
        IdListReq mpidList = new IdListReq(teamGoalList.stream()
                        .map(TeamGoal::getMemberPositionId)
                        .distinct()
                        .toList());
        ApiResponse<List<PositionDto>> response = memberClient.getPositionList(memberPositionId, mpidList);

        Map<UUID, PositionDto> positionMap = response.getData().stream()
                .collect(Collectors.toMap(PositionDto::getMemberPositionId, position -> position));

        return teamGoalList.stream().map(teamGoal -> {
            // Map에서 현재 teamGoal의 memberPositionId와 일치하는 PositionDto를 찾습니다.
            PositionDto matchingPosition = positionMap.get(teamGoal.getMemberPositionId());

            // TeamGoalResponseDto를 만들 때, 찾은 PositionDto의 데이터를 함께 넣어줍니다.
            return TeamGoalResponseDto.builder()
                    .teamGoalId(teamGoal.getId())
                    .title(teamGoal.getTitle())
                    .contents(teamGoal.getContents())
                    .startDate(teamGoal.getStartDate())
                    .endDate(teamGoal.getEndDate())
                    .memberPositionId(teamGoal.getMemberPositionId())
                    .memberName(matchingPosition != null ? matchingPosition.getMemberName() : null)
                    .memberPosition(matchingPosition != null ? matchingPosition.getTitleName() : null)
                    .memberOrganization(matchingPosition != null ? matchingPosition.getOrganizationName() : null)
                    .build();
        }).toList();
    }

    //    팀 목표 하위 목표 리스트
    public TeamGoalDetailResponseDto getSubGoal(UUID teamGoalId) {
        // 1. 팀 목표와 하위 목표 리스트를 조회합니다.
        TeamGoal teamGoal = teamGoalRepository.findById(teamGoalId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 팀목표 입니다."));
        List<Goal> subGoalList = performanceRepository.findByTeamGoal(teamGoal);

        // 👇 1. final로 선언하고, 할당은 if-else 블록 안에서 한 번만 하도록 변경
        final Map<UUID, PositionDto> positionMap;

        if (!subGoalList.isEmpty()) {
            IdListReq mpidList = new IdListReq(subGoalList.stream()
                    .map(Goal::getMemberPositionId)
                    .distinct()
                    .toList());

            ApiResponse<List<PositionDto>> response = memberClient.getPositionList(teamGoal.getMemberPositionId(), mpidList);

            if (response.isSuccess() && response.getData() != null) {
                positionMap = response.getData().stream()
                        .collect(Collectors.toMap(PositionDto::getMemberPositionId, position -> position));
            } else {
                positionMap = Collections.emptyMap(); // API 호출 실패 시 빈 맵 할당
            }
        } else {
            positionMap = Collections.emptyMap(); // 하위 목표가 없을 시 빈 맵 할당
        }

        // 이제 람다에서 final 변수인 positionMap을 안전하게 사용할 수 있습니다.
        List<GoalResponseDto> goalDtoList = subGoalList.stream().map(goal -> {
            PositionDto matchingPosition = positionMap.get(goal.getMemberPositionId());

            return GoalResponseDto.builder()
                    .goalId(goal.getId())
                    .title(goal.getTitle())
                    .contents(goal.getContents())
                    .memberPositionId(goal.getMemberPositionId())
                    .startDate(goal.getStartDate())
                    .endDate(goal.getEndDate())
                    .status(goal.getStatus())
                    .memberName(matchingPosition != null ? matchingPosition.getMemberName() : null)
                    .memberPostion(matchingPosition != null ? matchingPosition.getTitleName() : null)
                    .memberOrganization(matchingPosition != null ? matchingPosition.getOrganizationName() : null)
                    .build();
        }).toList();

        // 4. 최종 응답 DTO를 조립하여 반환합니다.
        return TeamGoalDetailResponseDto.builder()
                .title(teamGoal.getTitle())
                .contents(teamGoal.getContents())
                .startDate(teamGoal.getStartDate())
                .endDate(teamGoal.getEndDate())
                .memberPositionId(teamGoal.getMemberPositionId())
                .goalList(goalDtoList) // 변환된 DTO 리스트를 설정
                .build();
    }

    //    팀 목표 생성
    public UUID createTeamGoal(CreateTeamGoalDto dto, UUID memberPositionId) {
        TeamGoal newTeamGoal = TeamGoal.builder()
                .title(dto.getTitle())
                .contents(dto.getContents())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .memberPositionId(memberPositionId)
                .build();
        teamGoalRepository.save(newTeamGoal);

        return newTeamGoal.getId();
    }

    //    하위목표 상세조회
    public GoalResponseDto getGoalDetail(UUID id) {
        // 1. 목표(Goal) 엔티티를 조회합니다.
        Goal goal = performanceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 목표입니다."));

        // 2. 증적(Evidence) 리스트를 DTO 리스트로 변환합니다. (Stream 사용)
        List<EvidenceResponseDto> evidenceList = goal.getEvidenceList().stream()
                .map(evidence -> EvidenceResponseDto.builder()
                        .evidenceId(evidence.getId())
                        .evidenceUrl(evidence.getEvidenceUrl())
                        .build())
                .toList();

        // 3. FeignClient를 호출하여 담당자(Position) 정보를 조회합니다.
        ApiResponse<List<PositionDto>> response = memberClient.getPositionList(
                goal.getMemberPositionId(), // 헤더에 들어갈 ID
                new IdListReq(List.of(goal.getMemberPositionId())) // 쿼리 파라미터로 보낼 ID 리스트 (요소가 하나)
        );

        PositionDto positionInfo = null;
        // 4. 응답이 성공적이고 데이터가 비어있지 않은지 확인합니다.
        if (response.isSuccess() && response.getData() != null && !response.getData().isEmpty()) {
            // 리스트의 첫 번째(그리고 유일한) 결과를 가져옵니다.
            positionInfo = response.getData().get(0);
        }

        // 5. 최종 응답 DTO를 조립할 때, 위에서 조회한 positionInfo의 데이터를 함께 넣어줍니다.
        return GoalResponseDto.builder()
                .goalId(goal.getId())
                .title(goal.getTitle())
                .contents(goal.getContents())
                .memberPositionId(goal.getMemberPositionId())
                .startDate(goal.getStartDate())
                .endDate(goal.getEndDate())
                .status(goal.getStatus())
                .teamGoalTitle(goal.getTeamGoal().getTitle())
                .teamGoalContents(goal.getTeamGoal().getContents())
                .gradingSystem(goal.getGradingSystem())
                .evidenceList(evidenceList)
                // 조회한 PositionDto 데이터 추가
                .memberName(positionInfo != null ? positionInfo.getMemberName() : null)
                .memberPostion(positionInfo != null ? positionInfo.getTitleName() : null)
                .memberOrganization(positionInfo != null ? positionInfo.getOrganizationName() : null)
                .build();
    }

    //    내 목표 조회
    public List<GoalResponseDto> getMyGoal(UUID memberPositionId) {
        List<Goal> goalList = performanceRepository.findByMemberPositionId(memberPositionId);
//        List<Goal> goalList = performanceRepository.findAll();
        List<GoalResponseDto> dtoList = new ArrayList<>();
        for(Goal g : goalList) {
            GoalResponseDto dto = GoalResponseDto.builder()
                    .goalId(g.getId())
                    .title(g.getTitle())
                    .contents(g.getContents())
                    .memberPositionId(g.getMemberPositionId())
                    .status(g.getStatus())
                    .startDate(g.getStartDate())
                    .endDate(g.getEndDate())
                    .teamGoalTitle(g.getTeamGoal().getTitle())
                    .build();
            dtoList.add(dto);
        }

        return dtoList;
    }

    //    내 목표 생성
    public UUID createMyGoal(CreateMyGoalDto dto, UUID memberPositionId) {
        TeamGoal teamGoal = teamGoalRepository.findById(dto.getTeamGoalId()).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 팀목표 입니다."));

        Goal newGoal = Goal.builder()
                .title(dto.getTitle())
                .contents(dto.getContents())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .status(GoalStatus.REQUESTED)
                .teamGoal(teamGoal)
                .gradingSystem(dto.getGradingSystem())
                .memberPositionId(memberPositionId)
                .build();

        performanceRepository.save(newGoal);

        return newGoal.getId();
    }

    //    목표 상태변경(승인, 반려 등)
    public UUID updateGoalStatus(UpdateStatusDto dto) {
        Goal goal = performanceRepository.findById(dto.getGoalId()).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 목표입니다."));

        goal.updateStatus(dto.getStatus(), dto.getComment());

        return goal.getId();
    }

    //    평가 생성
    public UUID createEvaluation(CreateEvaluationDto dto, UUID memberPositionId) {
        Goal goal = performanceRepository.findById(dto.getGoalId()).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 목표입니다."));

        Evaluation evaluation = Evaluation.builder()
                .goal(goal)
                .grade(dto.getGrade())
                .type(dto.getType())
                .memberPositionId(memberPositionId)
                .comment(dto.getComment())
                .build();

        evaluationRepository.save(evaluation);

        return evaluation.getId();
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
    public UUID updateMyGoal(UpdateMyGoalDto dto) {
        Goal goal = performanceRepository.findById(dto.getGoalId()).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 목표입니다."));
        goal.updateGoal(dto);

        return goal.getId();
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
