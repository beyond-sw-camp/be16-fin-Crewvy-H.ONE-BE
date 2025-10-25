package com.crewvy.workforce_service.performance.service;

import com.crewvy.common.S3.S3Uploader;
import com.crewvy.common.dto.ApiResponse;
import com.crewvy.common.exception.BusinessException;
import com.crewvy.workforce_service.feignClient.MemberClient;
import com.crewvy.workforce_service.feignClient.dto.request.IdListReq;
import com.crewvy.workforce_service.feignClient.dto.response.PositionDto;
import com.crewvy.workforce_service.performance.constant.EvaluationType;
import com.crewvy.workforce_service.performance.constant.GoalStatus;
import com.crewvy.workforce_service.performance.constant.TeamGoalStatus;
import com.crewvy.workforce_service.performance.dto.request.*;
import com.crewvy.workforce_service.performance.dto.response.*;
import com.crewvy.workforce_service.performance.entity.*;
import com.crewvy.workforce_service.performance.repository.EvaluationRepository;
import com.crewvy.workforce_service.performance.repository.PerformanceRepository;
import com.crewvy.workforce_service.performance.repository.TeamGoalRepository;
import jakarta.persistence.EntityNotFoundException;
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
    private final TeamGoalCompletionService teamGoalCompletionService;

//    팀 목표 리스트
    public List<TeamGoalResponseDto> getTeamGoal(UUID memberPositionId) {
        List<TeamGoal> teamGoalList = teamGoalRepository.findAllByMemberPositionId(memberPositionId);

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
                    .status(teamGoal.getStatus().getCodeName())
                    .startDate(teamGoal.getStartDate())
                    .endDate(teamGoal.getEndDate())
                    .memberPositionId(teamGoal.getMemberPositionId())
                    .memberName(matchingPosition != null ? matchingPosition.getMemberName() : null)
                    .memberPosition(matchingPosition != null ? matchingPosition.getTitleName() : null)
                    .memberOrganization(matchingPosition != null ? matchingPosition.getOrganizationName() : null)
                    .build();
        }).toList();
    }

    //    팀 목표 리스트
    public List<TeamGoalResponseDto> getTeamGoalProcessing(UUID memberPositionId) {
        List<TeamGoal> teamGoalList = teamGoalRepository.findAllByMemberPositionIdAndStatus(memberPositionId, TeamGoalStatus.PROCESSING);

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
                    .status(teamGoal.getStatus().getCodeName())
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
        // 1. 팀 목표 엔티티를 조회합니다.
        TeamGoal teamGoal = teamGoalRepository.findByIdWithMembers(teamGoalId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 팀목표 입니다."));

        List<UUID> allMemberPositionIds = teamGoal.getTeamGoalMembers().stream()
                .map(TeamGoalMember::getMemberPositionId)
                .distinct()
                .toList();

        final Map<UUID, PositionDto> positionMap;
        if (!allMemberPositionIds.isEmpty()) {
            IdListReq mpidList = new IdListReq(allMemberPositionIds);

            // API 호출
            ApiResponse<List<PositionDto>> response = memberClient.getPositionList(teamGoal.getMemberPositionId(), mpidList);

            if (response.isSuccess() && response.getData() != null) {
                positionMap = response.getData().stream()
                        .collect(Collectors.toMap(PositionDto::getMemberPositionId, position -> position));
            } else {
                positionMap = Collections.emptyMap(); // API 호출 실패 시
            }
        } else {
            positionMap = Collections.emptyMap(); // 팀 멤버가 아무도 없을 시
        }

        // 4. 하위 목표 리스트를 조회합니다.
        List<Goal> subGoalList = performanceRepository.findByTeamGoal(teamGoal);

        // 5. 하위 목표 DTO 리스트를 변환합니다.
        List<GoalResponseDto> goalDtoList = subGoalList.stream().map(goal -> {
            PositionDto matchingPosition = positionMap.get(goal.getMemberPositionId());

            return GoalResponseDto.builder()
                    .goalId(goal.getId())
                    .title(goal.getTitle())
                    .contents(goal.getContents())
                    .memberPositionId(goal.getMemberPositionId())
                    .startDate(goal.getStartDate())
                    .endDate(goal.getEndDate())
                    .status(goal.getStatus().getCodeName())
                    .memberName(matchingPosition != null ? matchingPosition.getMemberName() : null)
                    .memberPostion(matchingPosition != null ? matchingPosition.getTitleName() : null)
                    .memberOrganization(matchingPosition != null ? matchingPosition.getOrganizationName() : null)
                    .build();
        }).toList();

        // 6. 팀 멤버 DTO 리스트를 변환합니다.
        List<TeamGoalMemberResDto> memberDtoList = teamGoal.getTeamGoalMembers().stream().map(member -> {
            PositionDto matchingPosition = positionMap.get(member.getMemberPositionId());

            return TeamGoalMemberResDto.builder()
                    .memberPositionId(member.getMemberPositionId())
                    .memberName(matchingPosition != null ? matchingPosition.getMemberName() : null)
                    .memberTitleName(matchingPosition != null ? matchingPosition.getTitleName() : null)
                    .memberOrganizationName(matchingPosition != null ? matchingPosition.getOrganizationName() : null)
                    .isCreater(member.getIsCreater())
                    .build();
        }).toList();


        // 7. 최종 응답 DTO를 조립하여 반환합니다.
        return TeamGoalDetailResponseDto.builder()
                .title(teamGoal.getTitle())
                .contents(teamGoal.getContents())
                .startDate(teamGoal.getStartDate())
                .endDate(teamGoal.getEndDate())
                .memberPositionId(teamGoal.getMemberPositionId())
                .goalList(goalDtoList)
                .memberList(memberDtoList)
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

        for(TeamGoalMemberReqDto t : dto.getMembers()) {
            TeamGoalMember teamGoalMember = TeamGoalMember.builder()
                    .teamGoal(newTeamGoal)
                    .memberPositionId(t.getMemberPositionId())
                    .isCreater(t.getIsCreater())
                    .build();
            newTeamGoal.getTeamGoalMembers().add(teamGoalMember);
        }
        teamGoalRepository.save(newTeamGoal);

//        목표 생성 이후 목표에 초대된 팀원들에게 알림 발송

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
                .status(goal.getStatus().getCodeName())
                .comment(goal.getComment())
                .teamGoalTitle(goal.getTeamGoal().getTitle())
                .teamGoalContents(goal.getTeamGoal().getContents())
                .teamGoalMemberPositionId(goal.getTeamGoal().getMemberPositionId())
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

        // 1. CANCELED 상태를 제외하고, N+1 문제도 해결된 메서드 호출
        List<Goal> goalList = performanceRepository.findActiveGoalsByMemberPositionIdWithTeamGoal(
                memberPositionId,
                GoalStatus.CANCELED // 제외할 상태
        );

        // 2. Stream을 사용한 DTO 변환 (for-loop 대체)
        return goalList.stream()
                .map(g -> GoalResponseDto.builder()
                        .goalId(g.getId())
                        .title(g.getTitle())
                        .contents(g.getContents())
                        .memberPositionId(g.getMemberPositionId())
                        .status(g.getStatus().getCodeName())
                        .startDate(g.getStartDate())
                        .endDate(g.getEndDate())
                        // Fetch Join을 했기 때문에 추가 쿼리 없이 바로 사용 가능
                        .teamGoalTitle(g.getTeamGoal() != null ? g.getTeamGoal().getTitle() : null)
                        .build())
                .toList();
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

        if(dto.getType().equals(EvaluationType.SELF)){
            goal.updateStatus(GoalStatus.SELF_EVAL_COMPLETED);
        }
        else if(dto.getType().equals(EvaluationType.SUPERVISOR)) {
            goal.updateStatus(GoalStatus.MANAGER_EVAL_COMPLETED);
            teamGoalCompletionService.checkAndCompleteTeamGoal(goal.getTeamGoal());
        }

        return evaluation.getId();
    }

    //    평가 조회
    public List<EvaluationResponseDto> findEvaluation(UUID goalId) {
        Goal goal = performanceRepository.findById(goalId).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 목표입니다."));
        List<Evaluation> evaluationList = evaluationRepository.findByGoal(goal);
        return evaluationList.stream().map(EvaluationResponseDto::from).toList();
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

//    팀 목표 수정
    public void updateTeamGoal(UUID id, CreateTeamGoalDto dto, UUID memberPositionId) {
        TeamGoal teamGoal = teamGoalRepository.findByIdWithMembers(id)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 팀 목표 입니다."));
        if(!teamGoal.getMemberPositionId().equals(memberPositionId)) {
            throw new BusinessException("수정 권한이 없습니다.");
        }

        teamGoal.updateTeamGoal(dto.getTitle(), dto.getContents(), dto.getStartDate(), dto.getEndDate());

        // 2. (성능 개선) 효율적인 컬렉션 업데이트

        // 2-1. 새로 전달된 멤버 ID 목록 (빠른 조회를 위해 Set 사용)
        Set<UUID> newMemberIds = dto.getMembers().stream()
                .map(TeamGoalMemberReqDto::getMemberPositionId)
                .collect(Collectors.toSet());

        // 2-2. 기존 멤버 목록
        List<TeamGoalMember> existingMembers = teamGoal.getTeamGoalMembers();

        // 2-3. 기존 멤버 ID 목록 (중복 추가 방지를 위해 Set 사용)
        Set<UUID> existingMemberIds = existingMembers.stream()
                .map(TeamGoalMember::getMemberPositionId)
                .collect(Collectors.toSet());

        // 2-4. [삭제] 기존 멤버 중 -> 새 목록에 없는 멤버를 제거
        // (Iterator를 사용해야 ConcurrentModificationException이 발생하지 않음)
        existingMembers.removeIf(member -> !newMemberIds.contains(member.getMemberPositionId()));

        // 2-5. [추가] 새 멤버 중 -> 기존 목록에 없는 멤버만 추가
        for (TeamGoalMemberReqDto m : dto.getMembers()) {
            if (!existingMemberIds.contains(m.getMemberPositionId())) {
                TeamGoalMember member = TeamGoalMember.builder()
                        .memberPositionId(m.getMemberPositionId())
                        .isCreater(m.getIsCreater()) // <-- (오타 참고)
                        .teamGoal(teamGoal)
                        .build();
                existingMembers.add(member); // teamGoal.getTeamGoalMembers()와 같은 참조입니다.
            }
        }
    }

    public void deleteTeamGoal(UUID id, UUID memberPositionId) {
        TeamGoal teamGoal = teamGoalRepository.findByIdWithGoals(id)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 팀 목표 입니다."));

        if(!teamGoal.getMemberPositionId().equals(memberPositionId)) {
            throw new BusinessException("수정 권한이 없습니다.");
        }

        teamGoal.updateStatus(TeamGoalStatus.CANCELED);

        teamGoal.getGoalList().forEach(goal -> goal.updateStatus(GoalStatus.CANCELED));
    }

//    평가대상 팀 목표 조회
    public List<TeamGoalResponseDto> findMyTeamGoalsToEvaluate(UUID memberPositionId) {
        // 1. 내가 관리자(생성자)인 '평가 대기' 팀 목표 조회
        List<TeamGoal> teamGoalList = teamGoalRepository.findAllByMemberPositionIdAndStatus(memberPositionId, TeamGoalStatus.AWAITING_EVALUATION);

        // 2. (최적화) 조회된 팀 목표가 없으면 API 호출 없이 바로 빈 리스트 반환
        if (teamGoalList.isEmpty()) {
            return new ArrayList<>(); // 또는 Collections.emptyList()
        }

        // 3. (수정) 나의 Position 정보 1건만 조회 (Map 불필요)
        ApiResponse<List<PositionDto>> response = memberClient.getPositionList(
                memberPositionId,
                new IdListReq(List.of(memberPositionId))
        );

        // 4. (수정) Map 대신 단일 PositionDto 객체로 관리
        // API 응답이 정상이면서, 데이터가 있고, 비어있지 않은지 확인
        PositionDto creatorInfo = null;
        if (response != null && response.getData() != null && !response.getData().isEmpty()) {
            creatorInfo = response.getData().get(0); // 1건만 있으므로 0번째 인덱스
        }

        // 5. DTO 변환 (람다에서 사용하기 위해 effectively final 변수 사용)
        final PositionDto finalCreatorInfo = creatorInfo;

        return teamGoalList.stream().map(teamGoal -> {
            // (수정) Map 조회 로직 삭제, finalCreatorInfo 변수 직접 사용
            return TeamGoalResponseDto.builder()
                    .teamGoalId(teamGoal.getId())
                    .title(teamGoal.getTitle())
                    .contents(teamGoal.getContents())
                    .status(teamGoal.getStatus().getCodeName())
                    .startDate(teamGoal.getStartDate())
                    .endDate(teamGoal.getEndDate())
                    .memberPositionId(teamGoal.getMemberPositionId()) // 이 ID는 항상 memberPositionId와 동일
                    .memberName(finalCreatorInfo != null ? finalCreatorInfo.getMemberName() : null)
                    .memberPosition(finalCreatorInfo != null ? finalCreatorInfo.getTitleName() : null)
                    .memberOrganization(finalCreatorInfo != null ? finalCreatorInfo.getOrganizationName() : null)
                    .build();
        }).toList();
    }

    //    본인평가 대상 내 목표 조회
    public List<GoalResponseDto> findMyGoalToEvaluate(UUID memberPositionId) {
        List<Goal> goalList = performanceRepository.findGoalsByMemberPositionIdAndStatus(
                memberPositionId,
                GoalStatus.AWAITING_EVALUATION // 제외할 상태
        );

        return goalList.stream()
                .map(g -> GoalResponseDto.builder()
                        .goalId(g.getId())
                        .title(g.getTitle())
                        .contents(g.getContents())
                        .memberPositionId(g.getMemberPositionId())
                        .status(g.getStatus().getCodeName())
                        .startDate(g.getStartDate())
                        .endDate(g.getEndDate())
                        .teamGoalTitle(g.getTeamGoal() != null ? g.getTeamGoal().getTitle() : null)
                        .build())
                .toList();
    }

//    평가완료 팀 목표 조회
    public List<TeamGoalResponseDto> findMyTeamGoalsComplete(UUID memberPositionId) {
        // 1. 내가 관리자(생성자)인 '평가 대기' 팀 목표 조회
        List<TeamGoal> teamGoalList = teamGoalRepository.findAllByMemberPositionIdAndStatus(memberPositionId, TeamGoalStatus.EVALUATION_COMPLETED);

        // 2. (최적화) 조회된 팀 목표가 없으면 API 호출 없이 바로 빈 리스트 반환
        if (teamGoalList.isEmpty()) {
            return new ArrayList<>(); // 또는 Collections.emptyList()
        }

        // 3. (수정) 나의 Position 정보 1건만 조회 (Map 불필요)
        ApiResponse<List<PositionDto>> response = memberClient.getPositionList(
                memberPositionId,
                new IdListReq(List.of(memberPositionId))
        );

        // 4. (수정) Map 대신 단일 PositionDto 객체로 관리
        // API 응답이 정상이면서, 데이터가 있고, 비어있지 않은지 확인
        PositionDto creatorInfo = null;
        if (response != null && response.getData() != null && !response.getData().isEmpty()) {
            creatorInfo = response.getData().get(0); // 1건만 있으므로 0번째 인덱스
        }

        // 5. DTO 변환 (람다에서 사용하기 위해 effectively final 변수 사용)
        final PositionDto finalCreatorInfo = creatorInfo;

        return teamGoalList.stream().map(teamGoal -> {
            // (수정) Map 조회 로직 삭제, finalCreatorInfo 변수 직접 사용
            return TeamGoalResponseDto.builder()
                    .teamGoalId(teamGoal.getId())
                    .title(teamGoal.getTitle())
                    .contents(teamGoal.getContents())
                    .status(teamGoal.getStatus().getCodeName())
                    .startDate(teamGoal.getStartDate())
                    .endDate(teamGoal.getEndDate())
                    .memberPositionId(teamGoal.getMemberPositionId()) // 이 ID는 항상 memberPositionId와 동일
                    .memberName(finalCreatorInfo != null ? finalCreatorInfo.getMemberName() : null)
                    .memberPosition(finalCreatorInfo != null ? finalCreatorInfo.getTitleName() : null)
                    .memberOrganization(finalCreatorInfo != null ? finalCreatorInfo.getOrganizationName() : null)
                    .build();
        }).toList();
    }

    //    평가 완료 내 목표 조회
    public List<GoalResponseDto> findMyGoalComplete(UUID memberPositionId) {
        List<Goal> goalList = performanceRepository.findGoalsByMemberPositionIdAndStatus(
                memberPositionId,
                GoalStatus.MANAGER_EVAL_COMPLETED
        );

        return goalList.stream()
                .map(g -> GoalResponseDto.builder()
                        .goalId(g.getId())
                        .title(g.getTitle())
                        .contents(g.getContents())
                        .memberPositionId(g.getMemberPositionId())
                        .status(g.getStatus().getCodeName())
                        .startDate(g.getStartDate())
                        .endDate(g.getEndDate())
                        .teamGoalTitle(g.getTeamGoal() != null ? g.getTeamGoal().getTitle() : null)
                        .build())
                .toList();
    }
}
