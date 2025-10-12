package com.crewvy.workspace_service.meeting.service;

import com.crewvy.common.entity.Bool;
import com.crewvy.common.exception.*;
import com.crewvy.workspace_service.meeting.constant.VideoConferenceStatus;
import com.crewvy.workspace_service.meeting.dto.*;
import com.crewvy.workspace_service.meeting.entity.VideoConference;
import com.crewvy.workspace_service.meeting.entity.VideoConferenceInvitee;
import com.crewvy.workspace_service.meeting.repository.VideoConferenceRepository;
import io.openvidu.java.client.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

// TODO : jwt가 이메일에서 UUID으로 변경되면 더미가 아니라 진짜 요청한 유저의 UUID를 넣어야 함
//  new UUID(123, 123) -> 진짜 UUID

@Slf4j
@Service
@Transactional
public class VideoConferenceService {
    private final VideoConferenceRepository videoConferenceRepository;
    private final OpenVidu openVidu;
    private final ConnectionProperties connectionProperties;

    public VideoConferenceService(VideoConferenceRepository videoConferenceRepository, OpenVidu openVidu, ConnectionProperties connectionProperties) {
        this.videoConferenceRepository = videoConferenceRepository;
        this.openVidu = openVidu;
        this.connectionProperties = connectionProperties;
    }

    public OpenViduSessionRes createVideoConference(VideoConferenceCreateReq videoConferenceCreateReq) {
        VideoConference videoConference = videoConferenceCreateReq.toEntity(new UUID(123, 123));

        videoConferenceCreateReq.getInviteeIdList().add(new UUID(123, 123));
        addInvitee(videoConference, videoConferenceCreateReq.getInviteeIdList());

        Session session;
        String token;
        try {
            session = openVidu.createSession();
            token = session.createConnection(connectionProperties).getToken();
        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            throw new RuntimeException("Error creating session", e);
        }

        videoConference.startVideoConference(session.getSessionId());
        videoConferenceRepository.save(videoConference);

        return OpenViduSessionRes.builder().sessionId(session.getSessionId()).token(token).build();
    }

    public VideoConferenceBookRes bookVideoConference(VideoConferenceCreateReq videoConferenceCreateReq) {
        VideoConference videoConference = videoConferenceCreateReq.toEntity(new UUID(123, 123));

        videoConferenceCreateReq.getInviteeIdList().add(new UUID(123, 123));
        addInvitee(videoConference, videoConferenceCreateReq.getInviteeIdList());

        videoConferenceRepository.save(videoConference);

        return VideoConferenceBookRes.fromEntity(videoConference);
    }

    public Page<VideoConferenceListRes> findAllMyVideoConference(UUID memberId, VideoConferenceStatus videoConferenceStatus, Pageable pageable) {

//        // TODO : 회의 시작 후 아무도 참여 안 하고 1시간 정도 흘러서 OpenVidu GC가 세션 정리한 경우가 있어서 일단 만들었는 데 필요한지 고민 해봐야할 듯
//        if (videoConferenceStatus == VideoConferenceStatus.IN_PROGRESS) {
//            try {
//                openVidu.fetch();
//            } catch (OpenViduJavaClientException | OpenViduHttpException e) {
//                throw new RuntimeException(e);
//            }
//            videoConferenceRepository.findByVideoConferenceInviteeList_MemberIdAndStatusFetchInvitees(new UUID(123, 123), videoConferenceStatus)
//                    .forEach(videoConference -> {
//                        if (openVidu.getActiveSession(videoConference.getSessionId()) == null) videoConference.endVideoConference();
//                    });
//        }

//        return videoConferenceRepository.findByVideoConferenceInviteeList_MemberIdAndStatus(new UUID(123, 123), videoConferenceStatus, pageable)
//                .map(VideoConferenceListRes::fromEntity);

        return videoConferenceRepository.findByVideoConferenceInviteeList_MemberIdAndStatusFetchInvitees(new UUID(123, 123), videoConferenceStatus, pageable)
                .map(VideoConferenceListRes::fromEntity);
    }

    public OpenViduSessionRes joinVideoConference(UUID videoConferenceId) {
        VideoConference videoConference = videoConferenceRepository.findById(videoConferenceId).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 화상회의 입니다."));

        if (videoConference.getStatus() != VideoConferenceStatus.IN_PROGRESS)
            throw new VideoConferenceNotInProgressException("진행 중인 화상회의가 아닙니다.");

        if (videoConference.getVideoConferenceInviteeList().stream().noneMatch(invitee -> invitee.getMemberId().equals(new UUID(123, 123))))
            throw new UserNotInvitedException("초대 받지 않은 화상회의입니다.");

        Session session = fetchAndGetActiveSession(videoConference);

        // 이미 접속 중인 유저 재 참여 방지
        boolean isAlreadyJoined = session
                .getActiveConnections().stream()
                .anyMatch(c -> c.getClientData().split(":")[0].equals(new UUID(123, 123).toString()));
        if (isAlreadyJoined) throw new UserAlreadyJoinedException("이미 참여 중인 화상회의입니다.");

        String token;
        try {
            token = session.createConnection(connectionProperties).getToken();
        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            throw new RuntimeException(e);
        }

        return OpenViduSessionRes.builder().sessionId(session.getSessionId()).token(token).build();
    }

    public OpenViduSessionRes startVideoConference(UUID videoConferenceId) {
        VideoConference videoConference = videoConferenceRepository.findById(videoConferenceId).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 화상회의 입니다."));

        if (videoConference.getStatus() != VideoConferenceStatus.WAITING)
            throw new VideoConferenceNotWaitingException("대기 중인 화상회의가 아닙니다.");

        if (!videoConference.getHostId().equals(new UUID(123, 123)))
            throw new UserNotHostException("화상회의의 호스트가 아닙니다.");

        String token;
        String sessionId;
        try {
            Session session = openVidu.createSession();

            sessionId = session.getSessionId();
            token = session.createConnection(connectionProperties).getToken();
        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            throw new RuntimeException(e);
        }

        videoConference.startVideoConference(sessionId);

        return OpenViduSessionRes.builder().sessionId(sessionId).token(token).build();
    }

    public VideoConferenceUpdateRes updateVideoConference(UUID videoConferenceId, VideoConferenceUpdateReq videoConferenceUpdateReq) {
        VideoConference videoConference = videoConferenceRepository.findById(videoConferenceId).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 화상회의 입니다."));

        if (!videoConference.getHostId().equals(new UUID(123, 123)))
            throw new UserNotHostException("화상회의의 호스트가 아닙니다.");

        if (videoConferenceUpdateReq.getName() != null)
            videoConference.updateName(videoConferenceUpdateReq.getName());

        if (videoConferenceUpdateReq.getDescription() != null)
            videoConference.updateDescription(videoConferenceUpdateReq.getDescription());

        if (videoConferenceUpdateReq.getIsRecording() != null)
            videoConference.updateIsRecording(videoConferenceUpdateReq.getIsRecording() ? Bool.TRUE : Bool.FALSE);

        if (videoConferenceUpdateReq.getScheduledStartTime() != null)
            videoConference.updateScheduledStartTime(LocalDateTime.parse(videoConferenceUpdateReq.getScheduledStartTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));

        videoConference.getVideoConferenceInviteeList().clear();
        videoConferenceUpdateReq.getInviteeIdList().add(new UUID(123, 123));
        addInvitee(videoConference, videoConferenceUpdateReq.getInviteeIdList());

        return VideoConferenceUpdateRes.fromEntity(videoConference);
    }

    public void deleteVideoConference(UUID videoConferenceId) {
        VideoConference videoConference = videoConferenceRepository.findById(videoConferenceId).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 화상회의 입니다."));

        if (videoConference.getStatus() != VideoConferenceStatus.WAITING)
            throw new VideoConferenceNotWaitingException("이미 회의가 진행 중이거나 종료되었습니다.");

        if (!videoConference.getHostId().equals(new UUID(123, 123)))
            throw new UserNotHostException("화상회의의 호스트가 아닙니다.");

        // TODO : soft-delete?
        videoConferenceRepository.delete(videoConference);
    }

    public void leaveVideoConference(UUID videoConferenceId) {
        VideoConference videoConference = videoConferenceRepository.findById(videoConferenceId).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 화상회의 입니다."));

        if (videoConference.getStatus() != VideoConferenceStatus.IN_PROGRESS)
            throw new VideoConferenceNotInProgressException("진행 중인 회의가 아닙니다.");

        Session session = fetchAndGetActiveSession(videoConference);
        Connection connection = session.getActiveConnections().stream()
                .filter(c -> c.getClientData().startsWith(new UUID(123, 123).toString())).findFirst()
                .orElseThrow(() -> new EntityNotFoundException("참여 중인 회의가 아닙니다."));

        try {
            session.forceDisconnect(connection);
        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            throw new RuntimeException(e);
        }
    }

    private Session fetchAndGetActiveSession(VideoConference videoConference) {
        try {
            openVidu.fetch();
        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            throw new RuntimeException(e);
        }

        return openVidu.getActiveSession(videoConference.getSessionId());
    }

    private void addInvitee(VideoConference videoConference, List<UUID> inviteeIdList) {
        inviteeIdList.stream()
                .map(id -> VideoConferenceInvitee.builder().memberId(id).videoConference(videoConference).build())
                .forEach(videoConference.getVideoConferenceInviteeList()::add);
    }

    private void sendOpenViduSignal(String sessionId, SignalReq signalReq) {
        Map<String, Object> body = new HashMap<>();
        body.put("session", sessionId);
        body.put("type", signalReq.getType());
        body.put("data", signalReq.getData());
        if (signalReq.getTo() != null && !signalReq.getTo().isEmpty()) {
            body.put("to", signalReq.getTo().stream().map(Connection::getConnectionId).toList());
        }

        webClient.post()
                .uri(openviduUrl + "/openvidu/api/signal")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString(("OPENVIDUAPP:" + openviduSecret).getBytes()))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                // There is a problem with some body parameter
                .onStatus(status -> status.value() == 400, response ->
                        response.bodyToMono(String.class).flatMap(errorBody ->
                                Mono.error(new InvalidSignalRequestException("시그널 전송 실패. 상태코드: 400, Body: " + errorBody))))
                // No session exists for the passed session body parameter
                .onStatus(status -> status.value() == 404, response ->
                        response.bodyToMono(String.class).flatMap(errorBody ->
                                Mono.error(new EntityNotFoundException("시그널 전송 실패. 상태코드: 404, Body: " + errorBody))))
                // No connection exists for the passed to array.
                // This error may be triggered if the session has no connected participants or if you provide some string value that does not correspond to a valid connectionId of the session (even though others may be correct)
                .onStatus(status -> status.value() == 406, response ->
                        response.bodyToMono(String.class).flatMap(errorBody ->
                                Mono.error(new ConnectionNotFoundException("시그널 전송 실패. 상태코드: 406, Body: " + errorBody))))
                // 기타 openvidu 문서에 명시되어 있지 않을 수 있는 에러 상태 코드 수신 시
                .onStatus(HttpStatusCode::isError, response ->
                        response.bodyToMono(String.class).flatMap(errorBody ->
                                Mono.error(new RuntimeException("시그널 전송 실패. 상태코드: " + response.statusCode() + ", Body: " + errorBody)))
                )
                .bodyToMono(Void.class)
                .block();
    }
}
