package com.crewvy.workspace_service.meeting.service;

import com.crewvy.common.entity.Bool;
import com.crewvy.common.exception.*;
import com.crewvy.workspace_service.meeting.constant.VideoConferenceStatus;
import com.crewvy.workspace_service.meeting.dto.*;
import com.crewvy.workspace_service.meeting.entity.Message;
import com.crewvy.workspace_service.meeting.entity.VideoConference;
import com.crewvy.workspace_service.meeting.entity.VideoConferenceInvitee;
import com.crewvy.workspace_service.meeting.repository.MessageRepository;
import com.crewvy.workspace_service.meeting.repository.VideoConferenceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.livekit.server.*;
import jakarta.persistence.EntityNotFoundException;
import livekit.LivekitEgress;
import livekit.LivekitModels.*;
import livekit.LivekitModels.DataPacket.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.internal.EverythingIsNonNull;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class VideoConferenceService {
    private final VideoConferenceRepository videoConferenceRepository;
    private final ObjectMapper objectMapper;
    private final MessageRepository messageRepository;
    private final RoomServiceClient roomServiceClient;
    private final EgressServiceClient egressServiceClient;
    private final LivekitEgress.S3Upload s3Upload;

    private final String LIVEKIT_API_KEY;
    private final String LIVEKIT_API_SECRET;

    public VideoConferenceService(VideoConferenceRepository videoConferenceRepository,
                                  LiveKitWebhookService liveKitWebhookService,
                                  ObjectMapper objectMapper,
                                  MessageRepository messageRepository,
                                  RoomServiceClient roomServiceClient,
                                  EgressServiceClient egressServiceClient,
                                  LivekitEgress.S3Upload s3Upload,
                                  @Value("${livekit.apiKey}") String LIVEKIT_API_KEY,
                                  @Value("${livekit.apiSecret}") String LIVEKIT_API_SECRET) {
        this.videoConferenceRepository = videoConferenceRepository;
        this.objectMapper = objectMapper;
        this.messageRepository = messageRepository;
        this.roomServiceClient = roomServiceClient;
        this.egressServiceClient = egressServiceClient;
        this.s3Upload = s3Upload;
        this.LIVEKIT_API_KEY = LIVEKIT_API_KEY;
        this.LIVEKIT_API_SECRET = LIVEKIT_API_SECRET;
    }

    public LiveKitSessionRes createVideoConference(UUID memberId, VideoConferenceCreateReq videoConferenceCreateReq) {
        VideoConference videoConference = videoConferenceCreateReq.toEntity(memberId);
        videoConferenceRepository.save(videoConference);

        videoConferenceCreateReq.getInviteeIdList().add(memberId);
        addInvitee(videoConference, videoConferenceCreateReq.getInviteeIdList());

        if (videoConferenceCreateReq.getIsRecording()) {
            createAutoEgressRoom(videoConference);
        }

        String token = createToken(videoConference.getId(), memberId);

        videoConference.startVideoConference();

        return LiveKitSessionRes.builder()
                .token(token)
                .videoConferenceId(videoConference.getId())
                .build();
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

    public LiveKitSessionRes joinVideoConference(UUID memberId, UUID videoConferenceId) {
        VideoConference videoConference = videoConferenceRepository.findById(videoConferenceId).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 화상회의 입니다."));

        if (videoConference.getStatus() != VideoConferenceStatus.IN_PROGRESS)
            throw new VideoConferenceNotInProgressException("진행 중인 화상회의가 아닙니다.");

        if (videoConference.getVideoConferenceInviteeSet().stream().noneMatch(invitee -> invitee.getMemberId().equals(memberId)))
            throw new UserNotInvitedException("초대 받지 않은 화상회의입니다.");

        Response<ParticipantInfo> res;
        try {
            res = roomServiceClient.getParticipant(videoConferenceId.toString(), memberId.toString()).execute();
        } catch (IOException e) {
            throw new LiveKitClientException("화상회의 통신 실패" + e.getMessage());
        }
        if (res.isSuccessful()) throw new UserAlreadyJoinedException("이미 참여 중인 화상회의 입니다.");

        String token = createToken(videoConferenceId, memberId);

        return LiveKitSessionRes.builder().videoConferenceId(videoConferenceId).token(token).build();
    }

    public LiveKitSessionRes startVideoConference(UUID memberId, UUID videoConferenceId) {
        VideoConference videoConference = videoConferenceRepository.findById(videoConferenceId).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 화상회의 입니다."));

        if (videoConference.getStatus() != VideoConferenceStatus.WAITING)
            throw new VideoConferenceNotWaitingException("대기 중인 화상회의가 아닙니다.");

        if (!videoConference.getHostId().equals(memberId))
            throw new UserNotHostException("화상회의의 호스트가 아닙니다.");

        if (videoConference.getIsRecording().toBoolean()) {
            createAutoEgressRoom(videoConference);
        }

        String token = createToken(videoConferenceId, memberId);

        videoConference.startVideoConference();

        return LiveKitSessionRes.builder().videoConferenceId(videoConferenceId).token(token).build();
    }

    public void sendMessage(UUID memberId, UUID videoConferenceId, ChatMessageReq chatMessageReq) {
        VideoConference videoConference = videoConferenceRepository.findById(videoConferenceId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 화상회의 입니다."));

        if (videoConference.getStatus() != VideoConferenceStatus.IN_PROGRESS)
            throw new VideoConferenceNotInProgressException("진행 중인 화상회의가 아닙니다.");

        if (!memberId.equals(chatMessageReq.getSenderId()))
            throw new InvalidSenderException("보내는 사람 ID가 올바르지 않습니다");

        Response<ParticipantInfo> res;
        try {
            res = roomServiceClient.getParticipant(videoConferenceId.toString(), memberId.toString()).execute();
        } catch (IOException e) {
            throw new LiveKitClientException("화상회의 통신 실패" + e.getMessage());
        }
        if (!res.isSuccessful() || ParticipantInfo.State.DISCONNECTED == Objects.requireNonNull(res.body()).getState())
            throw new InvalidSenderException("참여 중인 화상회의가 아닙니다.");

        String data;
        try {
            data = objectMapper.writeValueAsString(chatMessageReq);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // TODO : sse로 실패했다고 알려줄 지 고민해보기(화상회의에서 채팅이 매우 중요하진 않은 거 같음, UX가 저하되지만 다른 중요사항부터 개발하는 게 좋아보임)
        roomServiceClient.sendData(
                        videoConferenceId.toString(),
                        data.getBytes(),
                        Kind.RELIABLE,
                        Collections.emptyList(),
                        Collections.emptyList(),
                        "chat")
                .enqueue(new Callback<>() {
                    @Override
                    @EverythingIsNonNull
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Message message = Message.builder()
                                    .videoConference(videoConference)
                                    .senderId(chatMessageReq.getSenderId())
                                    .content(chatMessageReq.getContent())
                                    .build();

                            messageRepository.save(message);
                        }
                    }

                    @Override
                    @EverythingIsNonNull
                    public void onFailure(Call<Void> call, Throwable t) {
                        log.error(t.getMessage(), t);
                    }
                });
    }

    public Page<ChatMessageRes> findMessages(UUID memberId, UUID videoConferenceId, Pageable pageable) {
        VideoConference videoConference = videoConferenceRepository.findById(videoConferenceId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 화상회의 입니다."));

        if (videoConference.getStatus() != VideoConferenceStatus.IN_PROGRESS)
            throw new VideoConferenceNotInProgressException("진행 중인 화상회의가 아닙니다.");

        Response<ParticipantInfo> res;
        try {
            res = roomServiceClient.getParticipant(videoConferenceId.toString(), memberId.toString()).execute();
        } catch (IOException e) {
            throw new LiveKitClientException("화상회의 통신 실패" + e.getMessage());
        }
        if (!res.isSuccessful() || ParticipantInfo.State.DISCONNECTED == Objects.requireNonNull(res.body()).getState())
            throw new EntityNotFoundException("참여 중인 화상회의가 아닙니다.");

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

    private void addInvitee(VideoConference videoConference, List<UUID> inviteeIdList) {
        inviteeIdList.stream()
                .map(id -> VideoConferenceInvitee.builder().memberId(id).videoConference(videoConference).build())
                .forEach(videoConference.getVideoConferenceInviteeSet()::add);
    }

    private List<ParticipantInfo> getParticipantList(UUID videoConferenceId) {
        Call<List<ParticipantInfo>> call = roomServiceClient.listParticipants(videoConferenceId.toString());

        try {
            return call.execute().body();
        } catch (IOException e) {
            throw new LiveKitClientException("화상회의 통신 실패" + e.getMessage());
        }
    }

    private void createAutoEgressRoom(VideoConference videoConference) {
        try {
            roomServiceClient.createRoom(videoConference.getId().toString()).execute();
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new LiveKitClientException(e.getMessage());
        }

        LivekitEgress.EncodedFileOutput encodedFileOutput = LivekitEgress.EncodedFileOutput.newBuilder()
                .setFileType(LivekitEgress.EncodedFileType.MP4)
                .setFilepath("recordings/" + videoConference.getId() + "/" + videoConference.getName() + "-composite.mp4")
                .setS3(s3Upload)
                .build();

        try {
            egressServiceClient.startRoomCompositeEgress(
                            videoConference.getId().toString(),
                            encodedFileOutput,
                            "speaker")
                    .execute();
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new LiveKitClientException(e.getMessage());
        }
    }

    private String createToken(UUID videoConferenceId, UUID participantId) {

        AccessToken token = new AccessToken(LIVEKIT_API_KEY, LIVEKIT_API_SECRET);
        token.setIdentity(participantId.toString());
        token.addGrants(new RoomJoin(true), new RoomName(videoConferenceId.toString()));

        return token.toJwt();
    }
}
