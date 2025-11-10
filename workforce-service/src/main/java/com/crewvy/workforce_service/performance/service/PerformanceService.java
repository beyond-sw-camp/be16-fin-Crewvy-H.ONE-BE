package com.crewvy.workforce_service.performance.service;

import com.crewvy.common.S3.S3Uploader;
import com.crewvy.common.dto.ApiResponse;
import com.crewvy.common.dto.NotificationMessage;
import com.crewvy.common.kafka.KafkaMessagePublisher;
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
import jakarta.persistence.Id;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    private final ApplicationEventPublisher eventPublisher;
    private final TeamGoalCompletionService teamGoalCompletionService;

//    팀 목표 리스트
    public Page<TeamGoalResponseDto> getTeamGoal(UUID memberPositionId, String type, Pageable pageable) {

        Page<TeamGoal> teamGoalPage = null;
        if(type.equals("processing")) {
            teamGoalPage = teamGoalRepository.findAllByMemberPositionIdAndStatus(
                    memberPositionId, // 생성자(관리자) ID 기준
                    TeamGoalStatus.PROCESSING,
                    pageable // pageable 전달
            );
        }
        else {
            teamGoalPage = teamGoalRepository.findAllByMemberPositionIdAndStatus(
                    memberPositionId, // 생성자(관리자) ID 기준
                    TeamGoalStatus.EVALUATION_COMPLETED,
                    pageable // pageable 전달
            );
        }

        // 2. (수정) 현재 페이지의 내용(List<TeamGoal>) 가져오기
        List<TeamGoal> teamGoalListOnPage = teamGoalPage.getContent();

        // 3. (수정) 현재 페이지 생성자(관리자) 정보만 가져오도록 Map 준비
        Map<UUID, PositionDto> positionMap = new HashMap<>(); // final 제거, 초기화 방식 변경
        if (!teamGoalListOnPage.isEmpty()) { // teamGoalList -> teamGoalListOnPage
            // (수정) 현재 페이지의 생성자 ID 목록만 추출
            List<UUID> creatorIds = teamGoalListOnPage.stream()
                    .map(TeamGoal::getMemberPositionId) // 생성자 ID 추출
                    .distinct()
                    .toList();

            // (주의) memberClient.getPositionList 첫번째 파라미터 확인 필요
            ApiResponse<List<PositionDto>> response = memberClient.getPositionList(memberPositionId, new IdListReq(creatorIds));

            if (response.isSuccess() && response.getData() != null) {
                // (수정) memberPositionId 기준으로 Map 생성
                positionMap = response.getData().stream()
                        .collect(Collectors.toMap(PositionDto::getMemberPositionId, position -> position));
            }
        }

        // 4. (수정) Page.map()을 사용하여 Page<TeamGoal> -> Page<TeamGoalResponseDto> 변환
        final Map<UUID, PositionDto> finalPositionMap = positionMap; // 람다용 final 변수
        return teamGoalPage.map(teamGoal -> { // teamGoalList.stream() 대신 teamGoalPage.map() 사용
            PositionDto matchingPosition = finalPositionMap.get(teamGoal.getMemberPositionId());

            return TeamGoalResponseDto.builder()
                    .teamGoalId(teamGoal.getId())
                    .title(teamGoal.getTitle())
                    .contents(teamGoal.getContents())
                    .status(teamGoal.getStatus().getCodeName())
                    .startDate(teamGoal.getStartDate())
                    .endDate(teamGoal.getEndDate())
                    .memberPositionId(teamGoal.getMemberPositionId()) // 생성자 ID
                    .memberName(matchingPosition != null ? matchingPosition.getMemberName() : null)
                    .memberPosition(matchingPosition != null ? matchingPosition.getTitleName() : null)
                    .memberOrganization(matchingPosition != null ? matchingPosition.getOrganizationName() : null)
                    .build();
        });
    }

    //    팀 목표 리스트(개인 목표 추가시 선택가능한 팀 목표)
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
                .status(teamGoal.getStatus().getCodeName())
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
        List<PositionDto> position = memberClient.getPositionList(memberPositionId,
                new IdListReq(dto.getMembers().stream()
                        .map(TeamGoalMemberReqDto::getMemberPositionId)
                        .toList()
                )).getData();

        for(PositionDto p : position) {
            if(p.getMemberPositionId().equals(newTeamGoal.getMemberPositionId())) {
                continue;
            }
            NotificationMessage message = NotificationMessage.builder()
                    .memberId(p.getMemberId())
                    .notificationType("NT005")
                    .content("팀 목표 : " + newTeamGoal.getTitle() + "에 초대되었습니다.")
                    .targetId(newTeamGoal.getId())
                    .build();
            eventPublisher.publishEvent(message);
        }

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
                .teamGoalId(goal.getTeamGoal().getId())
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
    public Page<GoalResponseDto> getMyGoal(UUID memberPositionId, String type, Pageable pageable) {
        Page<Goal> goalPage = null;
        if(type.equals("approve")) {
            goalPage = performanceRepository.findActiveGoalsByMemberPositionIdWithTeamGoal(
                    memberPositionId,
                    GoalStatus.APPROVED,
                    pageable // pageable 전달
            );
        }
        else if(type.equals("request")) {
            goalPage = performanceRepository.findActiveGoalsByMemberPositionIdWithTeamGoal(
                    memberPositionId,
                    GoalStatus.REQUESTED,
                    pageable // pageable 전달
            );
        }
        else if(type.equals("reject")) {
            goalPage = performanceRepository.findActiveGoalsByMemberPositionIdWithTeamGoal(
                    memberPositionId,
                    GoalStatus.REJECTED,
                    pageable // pageable 전달
            );
        }
        else if(type.equals("complete")) {
            goalPage = performanceRepository.findActiveGoalsByMemberPositionIdWithTeamGoal(
                    memberPositionId,
                    GoalStatus.MANAGER_EVAL_COMPLETED,
                    pageable // pageable 전달
            );
        }

        // 2. (수정) Page.map()을 사용하여 Page<Goal> -> Page<GoalResponseDto> 변환
        return goalPage.map(g -> GoalResponseDto.builder() // goalList.stream() 대신 goalPage.map() 사용
                .goalId(g.getId())
                .title(g.getTitle())
                .contents(g.getContents())
                .memberPositionId(g.getMemberPositionId())
                .status(g.getStatus().getCodeName())
                .startDate(g.getStartDate())
                .endDate(g.getEndDate())
                // Fetch Join을 했기 때문에 추가 쿼리 없이 바로 사용 가능
                .teamGoalTitle(g.getTeamGoal() != null ? g.getTeamGoal().getTitle() : null)
                .build());
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

        List<PositionDto> position = memberClient.getPositionList(memberPositionId,
                new IdListReq(Arrays.asList(teamGoal.getMemberPositionId()))).getData();
        NotificationMessage message = NotificationMessage.builder()
                .memberId(position.get(0).getMemberId())
                .notificationType("NT007")
                .content("승인 요청 : 새로운 목표 " + newGoal.getTitle() + "가 승인 요청을 했습니다.")
                .targetId(newGoal.getId())
                .build();
        eventPublisher.publishEvent(message);

        return newGoal.getId();
    }

    //    목표 상태변경(승인, 반려 등)
    public UUID updateGoalStatus(UpdateStatusDto dto) {
        Goal goal = performanceRepository.findById(dto.getGoalId()).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 목표입니다."));

        goal.updateStatus(dto.getStatus(), dto.getComment());

        List<PositionDto> position = memberClient.getPositionList(goal.getMemberPositionId(),
                new IdListReq(Arrays.asList(goal.getMemberPositionId()))).getData();
        NotificationMessage message = NotificationMessage.builder()
                .memberId(position.get(0).getMemberId())
                .notificationType("NT006")
                .content("개인 목표 : " + goal.getTitle() + "이 " + dto.getStatus().getCodeName() + "되었습니다.")
                .targetId(goal.getId())
                .build();
        eventPublisher.publishEvent(message);

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

            List<PositionDto> position = memberClient.getPositionList(memberPositionId,
                    new IdListReq(Arrays.asList(goal.getTeamGoal().getMemberPositionId()))).getData();
            NotificationMessage message = NotificationMessage.builder()
                    .memberId(position.get(0).getMemberId())
                    .notificationType("NT009")
                    .targetId(goal.getId())
                    .content("평가 : " + goal.getTitle() + "의 본인평가가 완료되었습니다.")
                    .build();
            eventPublisher.publishEvent(message);
        }
        else if(dto.getType().equals(EvaluationType.SUPERVISOR)) {
            goal.updateStatus(GoalStatus.MANAGER_EVAL_COMPLETED);
            teamGoalCompletionService.checkAndCompleteTeamGoal(goal.getTeamGoal());

            List<PositionDto> position = memberClient.getPositionList(memberPositionId,
                    new IdListReq(Arrays.asList(goal.getMemberPositionId()))).getData();
            NotificationMessage message = NotificationMessage.builder()
                    .memberId(position.get(0).getMemberId())
                    .notificationType("NT008")
                    .targetId(goal.getId())
                    .content("평가 : " + goal.getTitle() + "의 평가가 모두 완료되었습니다.")
                    .build();
            eventPublisher.publishEvent(message);
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

        List<UUID> uuidList = new ArrayList<>();

        // 2-5. [추가] 새 멤버 중 -> 기존 목록에 없는 멤버만 추가
        for (TeamGoalMemberReqDto m : dto.getMembers()) {
            if (!existingMemberIds.contains(m.getMemberPositionId())) {
                TeamGoalMember member = TeamGoalMember.builder()
                        .memberPositionId(m.getMemberPositionId())
                        .isCreater(m.getIsCreater()) // <-- (오타 참고)
                        .teamGoal(teamGoal)
                        .build();
                existingMembers.add(member); // teamGoal.getTeamGoalMembers()와 같은 참조입니다.
                uuidList.add(m.getMemberPositionId());
            }
        }

        List<PositionDto> position = memberClient.getPositionList(memberPositionId,
                new IdListReq(uuidList)).getData();

        for(PositionDto p : position) {
            NotificationMessage message = NotificationMessage.builder()
                    .memberId(p.getMemberId())
                    .notificationType("NT005")
                    .content("팀 목표 : " + teamGoal.getTitle() + "에 초대되었습니다.")
                    .targetId(teamGoal.getId())
                    .build();
            eventPublisher.publishEvent(message);
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
    public Page<TeamGoalResponseDto> findMyTeamGoalsToEvaluate(UUID memberPositionId, Pageable pageable) {

        // 1. (수정) Repository 메서드에 pageable 전달, Page<TeamGoal> 반환
        Page<TeamGoal> teamGoalPage = teamGoalRepository.findAllByMemberPositionIdAndStatus2(
                memberPositionId, // 생성자(관리자) ID 기준
                TeamGoalStatus.AWAITING_EVALUATION,
                pageable // pageable 전달
        );

        // 2. (수정) 현재 페이지의 내용(List<TeamGoal>) 가져오기
        List<TeamGoal> teamGoalListOnPage = teamGoalPage.getContent();

        // 3. (수정) 현재 페이지 생성자(관리자) 정보만 조회 (Map 불필요)
        PositionDto creatorInfo = null; // 초기화
        if (!teamGoalListOnPage.isEmpty()) {
            // (최적화) 어차피 모든 TeamGoal의 생성자는 memberPositionId로 동일하므로
            // API는 딱 한 번만 호출하면 됩니다.
            ApiResponse<List<PositionDto>> response = memberClient.getPositionList(
                    memberPositionId,
                    new IdListReq(List.of(memberPositionId)) // 자기 자신의 ID만 전달
            );

            if (response != null && response.isSuccess() && response.getData() != null && !response.getData().isEmpty()) {
                creatorInfo = response.getData().get(0);
            }
        } else {
            // 조회된 목표가 없으면 빈 Page 반환
            return Page.empty(pageable);
        }

        // 4. DTO 변환 (람다에서 사용하기 위해 effectively final 변수 사용)
        final PositionDto finalCreatorInfo = creatorInfo;

        // 5. (수정) Page.map()을 사용하여 Page<TeamGoal> -> Page<TeamGoalResponseDto> 변환
        return teamGoalPage.map(teamGoal -> { // teamGoalList.stream() 대신 teamGoalPage.map() 사용
            // Map 조회 로직 삭제, finalCreatorInfo 변수 직접 사용
            return TeamGoalResponseDto.builder()
                    .teamGoalId(teamGoal.getId())
                    .title(teamGoal.getTitle())
                    .contents(teamGoal.getContents())
                    .status(teamGoal.getStatus().getCodeName())
                    .startDate(teamGoal.getStartDate())
                    .endDate(teamGoal.getEndDate())
                    .memberPositionId(teamGoal.getMemberPositionId()) // 생성자 ID
                    .memberName(finalCreatorInfo != null ? finalCreatorInfo.getMemberName() : null)
                    .memberPosition(finalCreatorInfo != null ? finalCreatorInfo.getTitleName() : null)
                    .memberOrganization(finalCreatorInfo != null ? finalCreatorInfo.getOrganizationName() : null)
                    .build();
        });
    }

    //    본인평가 대상 내 목표 조회
    public Page<GoalResponseDto> findMyGoalToEvaluate(UUID memberPositionId, Pageable pageable) {

        // 1. (수정) Repository 메서드에 pageable 전달, Page<Goal> 반환
        Page<Goal> goalPage = performanceRepository.findGoalsByMemberPositionIdAndStatus(
                memberPositionId,
                GoalStatus.AWAITING_EVALUATION, // 평가 대기 상태 조회
                pageable // pageable 전달
        );

        // 2. (수정) Page.map()을 사용하여 Page<Goal> -> Page<GoalResponseDto> 변환
        return goalPage.map(g -> GoalResponseDto.builder() // goalList.stream() 대신 goalPage.map() 사용
                .goalId(g.getId())
                .title(g.getTitle())
                .contents(g.getContents())
                .memberPositionId(g.getMemberPositionId())
                .status(g.getStatus().getCodeName())
                .startDate(g.getStartDate())
                .endDate(g.getEndDate())
                // Fetch Join을 했는지 여부에 따라 추가 쿼리 발생 가능성 있음
                .teamGoalTitle(g.getTeamGoal() != null ? g.getTeamGoal().getTitle() : null)
                .build());
    }

//    평가완료 팀 목표 조회
    public Page<TeamGoalResponseDto> findMyTeamGoalsComplete(UUID memberPositionId, Pageable pageable) {

        // 1. (수정) Repository 메서드에 pageable 전달, Page<TeamGoal> 반환
        Page<TeamGoal> teamGoalPage = teamGoalRepository.findAllByMemberPositionIdAndStatus2(
                memberPositionId, // 생성자(관리자) ID 기준
                TeamGoalStatus.EVALUATION_COMPLETED, // 평가 완료 상태
                pageable // pageable 전달
        );

        // 2. (수정) 현재 페이지의 내용(List<TeamGoal>) 가져오기
        List<TeamGoal> teamGoalListOnPage = teamGoalPage.getContent();

        // 3. (수정) 현재 페이지 생성자(관리자) 정보만 조회 (Map 불필요)
        PositionDto creatorInfo = null; // 초기화
        if (!teamGoalListOnPage.isEmpty()) {
            // (최적화) 어차피 모든 TeamGoal의 생성자는 memberPositionId로 동일하므로
            // API는 딱 한 번만 호출하면 됩니다.
            ApiResponse<List<PositionDto>> response = memberClient.getPositionList(
                    memberPositionId,
                    new IdListReq(List.of(memberPositionId)) // 자기 자신의 ID만 전달
            );

            if (response != null && response.isSuccess() && response.getData() != null && !response.getData().isEmpty()) {
                creatorInfo = response.getData().get(0);
            }
        } else {
            // 조회된 목표가 없으면 빈 Page 반환
            return Page.empty(pageable);
        }

        // 4. DTO 변환 (람다에서 사용하기 위해 effectively final 변수 사용)
        final PositionDto finalCreatorInfo = creatorInfo;

        // 5. (수정) Page.map()을 사용하여 Page<TeamGoal> -> Page<TeamGoalResponseDto> 변환
        return teamGoalPage.map(teamGoal -> { // teamGoalList.stream() 대신 teamGoalPage.map() 사용
            // Map 조회 로직 삭제, finalCreatorInfo 변수 직접 사용
            return TeamGoalResponseDto.builder()
                    .teamGoalId(teamGoal.getId())
                    .title(teamGoal.getTitle())
                    .contents(teamGoal.getContents())
                    .status(teamGoal.getStatus().getCodeName())
                    .startDate(teamGoal.getStartDate())
                    .endDate(teamGoal.getEndDate())
                    .memberPositionId(teamGoal.getMemberPositionId()) // 생성자 ID
                    .memberName(finalCreatorInfo != null ? finalCreatorInfo.getMemberName() : null)
                    .memberPosition(finalCreatorInfo != null ? finalCreatorInfo.getTitleName() : null)
                    .memberOrganization(finalCreatorInfo != null ? finalCreatorInfo.getOrganizationName() : null)
                    .build();
        });
    }

    //    평가 완료 내 목표 조회
    public Page<GoalResponseDto> findMyGoalComplete(UUID memberPositionId, Pageable pageable) {

        // 1. (수정) Repository 메서드에 pageable 전달, Page<Goal> 반환
        Page<Goal> goalPage = performanceRepository.findGoalsByMemberPositionIdAndStatus(
                memberPositionId,
                GoalStatus.MANAGER_EVAL_COMPLETED, // 최종 평가 완료 상태
                pageable // pageable 전달
        );

        // 2. (수정) Page.map()을 사용하여 Page<Goal> -> Page<GoalResponseDto> 변환
        return goalPage.map(g -> GoalResponseDto.builder() // goalList.stream() 대신 goalPage.map() 사용
                .goalId(g.getId())
                .title(g.getTitle())
                .contents(g.getContents())
                .memberPositionId(g.getMemberPositionId())
                .status(g.getStatus().getCodeName())
                .startDate(g.getStartDate())
                .endDate(g.getEndDate())
                // Fetch Join을 했는지 여부에 따라 추가 쿼리 발생 가능성 있음
                .teamGoalTitle(g.getTeamGoal() != null ? g.getTeamGoal().getTitle() : null)
                .build());
    }

    public EvaluationStatResDto getStat(UUID memberPositionId) {
        int myGoalCount = performanceRepository.countGoalsByMemberPositionIdAndStatus(
                        memberPositionId,
                        GoalStatus.AWAITING_EVALUATION
        );
        int teamGoalCount = teamGoalRepository.countByMemberPositionIdAndStatus2(
                        memberPositionId,
                        TeamGoalStatus.AWAITING_EVALUATION
        );
        int myGoalCompleteCount = performanceRepository.countGoalsByMemberPositionIdAndStatus(
                memberPositionId,
                GoalStatus.MANAGER_EVAL_COMPLETED
        );
        int teamGoalCompleteCount = teamGoalRepository.countByMemberPositionIdAndStatus2(
                memberPositionId,
                TeamGoalStatus.EVALUATION_COMPLETED
        );;

        return EvaluationStatResDto.builder()
                .myGoalCount(myGoalCount)
                .teamGoalCount(teamGoalCount)
                .myGoalCompleteCount(myGoalCompleteCount)
                .teamGoalCompleteCount(teamGoalCompleteCount)
                .build();
    }
}
