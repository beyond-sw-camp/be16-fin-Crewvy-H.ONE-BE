package com.crewvy.workspace_service.meeting.service;

import com.crewvy.common.entity.Bool;
import com.crewvy.common.exception.*;
import com.crewvy.workspace_service.meeting.constant.VideoConferenceStatus;
import com.crewvy.workspace_service.meeting.dto.*;
import com.crewvy.workspace_service.meeting.dto.openvidu.SignalReq;
import com.crewvy.workspace_service.meeting.entity.Message;
import com.crewvy.workspace_service.meeting.entity.VideoConference;
import com.crewvy.workspace_service.meeting.entity.VideoConferenceInvitee;
import com.crewvy.workspace_service.meeting.repository.MessageRepository;
import com.crewvy.workspace_service.meeting.repository.VideoConferenceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.openvidu.java.client.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Slf4j
@Service
@Transactional
public class VideoConferenceService {
    private final VideoConferenceRepository videoConferenceRepository;
    private final OpenVidu openVidu;
    private final ConnectionProperties connectionProperties;
    private final WebClient webClient;
    private final String openviduUrl;
    private final String openviduSecret;
    private final ObjectMapper objectMapper;
    private final MessageRepository messageRepository;

    public VideoConferenceService(VideoConferenceRepository videoConferenceRepository,
                                  OpenVidu openVidu,
                                  ConnectionProperties connectionProperties,
                                  WebClient.Builder webClientBuilder,
                                  @Value("${openvidu.url}") String openviduUrl,
                                  @Value("${openvidu.secret}") String openviduSecret, ObjectMapper objectMapper, MessageRepository messageRepository) {
        this.videoConferenceRepository = videoConferenceRepository;
        this.openVidu = openVidu;
        this.connectionProperties = connectionProperties;
        this.webClient = webClientBuilder.build();
        this.openviduUrl = openviduUrl;
        this.openviduSecret = openviduSecret;
        this.objectMapper = objectMapper;
        this.messageRepository = messageRepository;
    }

    public OpenViduSessionRes createVideoConference(UUID memberId, VideoConferenceCreateReq videoConferenceCreateReq) {
        VideoConference videoConference = videoConferenceCreateReq.toEntity(memberId);
        videoConferenceRepository.save(videoConference);

        videoConferenceCreateReq.getInviteeIdList().add(memberId);
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

        return OpenViduSessionRes.builder().sessionId(session.getSessionId()).token(token).videoConferenceId(videoConference.getId()).build();
    }

    public VideoConferenceBookRes bookVideoConference(UUID memberId, VideoConferenceCreateReq videoConferenceCreateReq) {
        VideoConference videoConference = videoConferenceCreateReq.toEntity(memberId);
        videoConferenceRepository.save(videoConference);

        videoConferenceCreateReq.getInviteeIdList().add(memberId);
        addInvitee(videoConference, videoConferenceCreateReq.getInviteeIdList());

        return VideoConferenceBookRes.fromEntity(videoConference);
    }

    public Page<VideoConferenceListRes> findAllMyVideoConference(UUID memberId, VideoConferenceStatus videoConferenceStatus, Pageable pageable) {
        return videoConferenceRepository.findByVideoConferenceInviteeList_MemberIdAndStatusFetchInvitees(memberId, videoConferenceStatus, pageable)
                .map(VideoConferenceListRes::fromEntity);
    }

    public OpenViduSessionRes joinVideoConference(UUID memberId, UUID videoConferenceId) {
        VideoConference videoConference = videoConferenceRepository.findById(videoConferenceId).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 화상회의 입니다."));

        if (videoConference.getStatus() != VideoConferenceStatus.IN_PROGRESS)
            throw new VideoConferenceNotInProgressException("진행 중인 화상회의가 아닙니다.");

        if (videoConference.getVideoConferenceInviteeSet().stream().noneMatch(invitee -> invitee.getMemberId().equals(memberId)))
            throw new UserNotInvitedException("초대 받지 않은 화상회의입니다.");

        Session session = fetchAndGetActiveSession(videoConference);

        // 이미 접속 중인 유저 재 참여 방지
        boolean isAlreadyJoined = session
                .getActiveConnections().stream()
                .anyMatch(c -> c.getClientData().split(":")[0].equals(memberId.toString()));
        if (isAlreadyJoined) throw new UserAlreadyJoinedException("이미 참여 중인 화상회의입니다.");

        String token;
        try {
            token = session.createConnection(connectionProperties).getToken();
        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            throw new RuntimeException(e);
        }

        return OpenViduSessionRes.builder().sessionId(session.getSessionId()).token(token).build();
    }

    public OpenViduSessionRes startVideoConference(UUID memberId, UUID videoConferenceId) {
        VideoConference videoConference = videoConferenceRepository.findById(videoConferenceId).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 화상회의 입니다."));

        if (videoConference.getStatus() != VideoConferenceStatus.WAITING)
            throw new VideoConferenceNotWaitingException("대기 중인 화상회의가 아닙니다.");

        if (!videoConference.getHostId().equals(memberId))
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

    public void sendMessage(UUID memberId, UUID videoConferenceId, ChatMessageReq chatMessageReq) {
        VideoConference videoConference = videoConferenceRepository.findById(videoConferenceId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 화상회의 입니다."));

        if (videoConference.getStatus() != VideoConferenceStatus.IN_PROGRESS)
            throw new VideoConferenceNotInProgressException("진행 중인 화상회의가 아닙니다.");

        if (!memberId.equals(chatMessageReq.getSenderId()))
            throw new InvalidSenderException("보내는 사람 ID가 올바르지 않습니다");

        Message message = Message.builder()
                .videoConference(videoConference)
                .senderId(chatMessageReq.getSenderId())
                .content(chatMessageReq.getContent())
                .build();

        messageRepository.save(message);

        Session session = fetchAndGetActiveSession(videoConference);

        if (session.getActiveConnections().stream().noneMatch(c -> c.getClientData().split(":")[0].equals(memberId.toString())))
            throw new InvalidSenderException("참여 중인 화상회의가 아닙니다.");

        String data;
        try {
            data = objectMapper.writeValueAsString(chatMessageReq);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        SignalReq signalReq = SignalReq.builder()
                .type("chat")
                .data(data)
                .build();

        sendOpenViduSignal(session.getSessionId(), signalReq);
    }

    public Page<ChatMessageRes> findMessages(UUID memberId, UUID videoConferenceId, Pageable pageable) {
        VideoConference videoConference = videoConferenceRepository.findById(videoConferenceId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 화상회의 입니다."));

        if (videoConference.getStatus() != VideoConferenceStatus.IN_PROGRESS)
            throw new VideoConferenceNotInProgressException("진행 중인 화상회의가 아닙니다.");

        Session session = fetchAndGetActiveSession(videoConference);

        if (session.getActiveConnections().stream().noneMatch(c -> c.getClientData().split(":")[0].equals(memberId.toString())))
            throw new InvalidSenderException("참여 중인 화상회의가 아닙니다.");

        return messageRepository.findByVideoConference(videoConference, pageable)
                .map(ChatMessageRes::fromEntity);
    }

    public VideoConferenceUpdateRes updateVideoConference(UUID memberId, UUID videoConferenceId, VideoConferenceUpdateReq videoConferenceUpdateReq) {
        VideoConference videoConference = videoConferenceRepository.findById(videoConferenceId).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 화상회의 입니다."));

        if (!videoConference.getHostId().equals(memberId))
            throw new UserNotHostException("화상회의의 호스트가 아닙니다.");

        if (videoConferenceUpdateReq.getName() != null)
            videoConference.updateName(videoConferenceUpdateReq.getName());

        if (videoConferenceUpdateReq.getDescription() != null)
            videoConference.updateDescription(videoConferenceUpdateReq.getDescription());

        if (videoConferenceUpdateReq.getIsRecording() != null)
            videoConference.updateIsRecording(Bool.fromBoolean(videoConferenceUpdateReq.getIsRecording()));

        if (videoConferenceUpdateReq.getScheduledStartTime() != null)
            videoConference.updateScheduledStartTime(videoConferenceUpdateReq.getScheduledStartTime());

        videoConference.getVideoConferenceInviteeSet().clear();
        videoConferenceUpdateReq.getInviteeIdList().add(memberId);
        addInvitee(videoConference, videoConferenceUpdateReq.getInviteeIdList());

        return VideoConferenceUpdateRes.fromEntity(videoConference);
    }

    public void deleteVideoConference(UUID memberId, UUID videoConferenceId) {
        VideoConference videoConference = videoConferenceRepository.findById(videoConferenceId).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 화상회의 입니다."));

        if (videoConference.getStatus() != VideoConferenceStatus.WAITING)
            throw new VideoConferenceNotWaitingException("이미 회의가 진행 중이거나 종료되었습니다.");

        if (!videoConference.getHostId().equals(memberId))
            throw new UserNotHostException("화상회의의 호스트가 아닙니다.");

        // TODO : soft-delete?
        videoConferenceRepository.delete(videoConference);
    }

    private Session fetchAndGetActiveSession(VideoConference videoConference) {
        try {
            openVidu.fetch();
        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            throw new RuntimeException(e);
        }

        Session session = openVidu.getActiveSession(videoConference.getSessionId());
        if (session == null) throw new EntityNotFoundException("활성화되지 않은 세션입니다.");

        return session;
    }

    private void addInvitee(VideoConference videoConference, List<UUID> inviteeIdList) {
        inviteeIdList.stream()
                .map(id -> VideoConferenceInvitee.builder().memberId(id).videoConference(videoConference).build())
                .forEach(videoConference.getVideoConferenceInviteeSet()::add);
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
