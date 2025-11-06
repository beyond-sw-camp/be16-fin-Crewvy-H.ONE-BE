package com.crewvy.workspace_service.meeting.service;

import com.crewvy.common.aes.AESUtil;
import com.crewvy.common.dto.NotificationMessage;
import com.crewvy.common.dto.ScheduleDeleteDto;
import com.crewvy.common.dto.ScheduleDto;
import com.crewvy.common.entity.Bool;
import com.crewvy.common.event.MinuteSavedEvent;
import com.crewvy.common.exception.*;
import com.crewvy.workspace_service.feignClient.MemberFeignClient;
import com.crewvy.workspace_service.feignClient.dto.IdListReq;
import com.crewvy.workspace_service.feignClient.dto.MemberNameListRes;
import com.crewvy.workspace_service.meeting.constant.MinuteStatus;
import com.crewvy.workspace_service.meeting.constant.VideoConferenceStatus;
import com.crewvy.workspace_service.meeting.dto.*;
import com.crewvy.workspace_service.meeting.dto.ai.TranscribeRes;
import com.crewvy.workspace_service.meeting.entity.*;
import com.crewvy.workspace_service.meeting.repository.MessageRepository;
import com.crewvy.workspace_service.meeting.repository.MinuteRepository;
import com.crewvy.workspace_service.meeting.repository.MinuteSearchOutboxEventRepository;
import com.crewvy.workspace_service.meeting.repository.VideoConferenceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.livekit.server.*;
import jakarta.persistence.EntityNotFoundException;
import livekit.LivekitEgress;
import livekit.LivekitModels.DataPacket.Kind;
import livekit.LivekitModels.ParticipantInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.internal.EverythingIsNonNull;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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
    private final MemberFeignClient memberFeignClient;
    private final ApplicationEventPublisher eventPublisher;

    private final String LIVEKIT_API_KEY;
    private final String LIVEKIT_API_SECRET;
    private final MinuteRepository minuteRepository;
    private final AESUtil aesUtil;
    private final MinuteSearchOutboxEventRepository minuteSearchOutboxEventRepository;

    public VideoConferenceService(VideoConferenceRepository videoConferenceRepository,
                                  LiveKitWebhookService liveKitWebhookService,
                                  ObjectMapper objectMapper,
                                  MessageRepository messageRepository,
                                  RoomServiceClient roomServiceClient,
                                  EgressServiceClient egressServiceClient,
                                  LivekitEgress.S3Upload s3Upload,
                                  @Value("${livekit.apiKey}") String LIVEKIT_API_KEY,
                                  @Value("${livekit.apiSecret}") String LIVEKIT_API_SECRET,
                                  MinuteRepository minuteRepository,
                                  AESUtil aesUtil,
                                  MemberFeignClient memberFeignClient, ApplicationEventPublisher eventPublisher, MinuteSearchOutboxEventRepository minuteSearchOutboxEventRepository) {
        this.videoConferenceRepository = videoConferenceRepository;
        this.objectMapper = objectMapper;
        this.messageRepository = messageRepository;
        this.roomServiceClient = roomServiceClient;
        this.egressServiceClient = egressServiceClient;
        this.s3Upload = s3Upload;
        this.LIVEKIT_API_KEY = LIVEKIT_API_KEY;
        this.LIVEKIT_API_SECRET = LIVEKIT_API_SECRET;
        this.minuteRepository = minuteRepository;
        this.aesUtil = aesUtil;
        this.memberFeignClient = memberFeignClient;
        this.eventPublisher = eventPublisher;
        this.minuteSearchOutboxEventRepository = minuteSearchOutboxEventRepository;
    }

    public LiveKitSessionRes createVideoConference(UUID memberId, String memberName, VideoConferenceCreateReq videoConferenceCreateReq) {
        VideoConference videoConference = videoConferenceCreateReq.toEntity(memberId);
        videoConferenceRepository.save(videoConference);

        videoConferenceCreateReq.getInviteeIdList().add(memberId);
        addInvitee(videoConference, videoConferenceCreateReq.getInviteeIdList());

        if (videoConferenceCreateReq.getIsRecording()) {
            createAutoEgressRoom(videoConference);
        }

        String token = createToken(videoConference.getId(), memberId, memberName);

        videoConference.startVideoConference();

        return LiveKitSessionRes.builder()
                .token(token)
                .videoConferenceId(videoConference.getId())
                .build();
    }

    public VideoConferenceBookRes bookVideoConference(UUID memberId, VideoConferenceCreateReq videoConferenceCreateReq) {
        VideoConference videoConference = videoConferenceCreateReq.toEntity(memberId);


        videoConferenceCreateReq.getInviteeIdList().add(memberId);
        addInvitee(videoConference, videoConferenceCreateReq.getInviteeIdList());

        videoConferenceRepository.save(videoConference);

        for(VideoConferenceInvitee invitee : videoConference.getVideoConferenceInviteeSet()) {
            ScheduleDto schedule = ScheduleDto.builder()
                    .memberId(invitee.getMemberId())
                    .originId(invitee.getId())
                    .title(videoConference.getName())
                    .contents(videoConference.getDescription())
                    .startDate(videoConference.getScheduledStartTime())
                    .type("CT001")
                    .build();

            eventPublisher.publishEvent(schedule);

            if(!invitee.getMemberId().equals(memberId)) {
                NotificationMessage message = NotificationMessage.builder()
                        .memberId(invitee.getMemberId())
                        .targetId(videoConference.getId())
                        .notificationType("NT001")
                        .content("화상회의 : " + videoConference.getName() + "에 초대되었습니다.")
                        .build();

                eventPublisher.publishEvent(message);
            }
        }

        return VideoConferenceBookRes.fromEntity(videoConference);
    }

    public Page<VideoConferenceListRes> findAllMyVideoConference(UUID memberId, VideoConferenceStatus videoConferenceStatus, Pageable pageable) {
        Page<VideoConference> videoConferencePage = videoConferenceRepository.findByVideoConferenceInviteeList_MemberIdAndStatusFetchInvitees(memberId, videoConferenceStatus, pageable);

        List<UUID> idList = new ArrayList<>(videoConferencePage.map(VideoConference::getHostId).toList());

        if (videoConferenceStatus == VideoConferenceStatus.WAITING) {
            videoConferencePage.forEach(vc -> idList.addAll(vc.getVideoConferenceInviteeSet().stream().map(VideoConferenceInvitee::getMemberId).toList()));
        }

        Map<UUID, String> memberName = findMemberName(idList.stream().distinct().toList());

        if (videoConferenceStatus == VideoConferenceStatus.IN_PROGRESS) {
            return videoConferencePage.map(vc -> {
                int participantsCount = 0;
                try {
                    participantsCount = roomServiceClient.listParticipants(String.valueOf(vc.getId())).execute().body().size();
                } catch (Exception e) {
                    log.warn("Error getting participants count for video conference {}", vc.getId(), e);
                }
                return VideoConferenceListRes.fromEntityWithParticipantsCnt(vc, memberName.getOrDefault(vc.getHostId(), "알 수 없는 호스트"), participantsCount);
            });
        }


        return videoConferencePage
                .map(vc -> VideoConferenceListRes.fromEntity(
                        vc,
                        memberName.getOrDefault(vc.getHostId(), "알 수 없는 호스트"),
                        vc.getVideoConferenceInviteeSet().stream().map(vci -> VideoConferenceInviteeRes.fromEntity(vci, memberName.getOrDefault(vci.getMemberId(), "알 수 없는 참여자"))).toList())
                );
    }

    public LiveKitSessionRes joinVideoConference(UUID memberId, String memberName, UUID videoConferenceId) {
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

        String token = createToken(videoConferenceId, memberId, memberName);

        return LiveKitSessionRes.builder().videoConferenceId(videoConferenceId).token(token).build();
    }

    public LiveKitSessionRes startVideoConference(UUID memberId, String memberName, UUID videoConferenceId) {
        VideoConference videoConference = videoConferenceRepository.findById(videoConferenceId).orElseThrow(() -> new EntityNotFoundException("존재하지 않는 화상회의 입니다."));

        if (videoConference.getStatus() != VideoConferenceStatus.WAITING)
            throw new VideoConferenceNotWaitingException("대기 중인 화상회의가 아닙니다.");

        if (!videoConference.getHostId().equals(memberId))
            throw new UserNotHostException("화상회의의 호스트가 아닙니다.");

        if (videoConference.getIsRecording().toBoolean()) {
            createAutoEgressRoom(videoConference);
        }

        String token = createToken(videoConferenceId, memberId, memberName);

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

//        return messageRepository.findByVideoConference(videoConference, pageable)
//                .map(ChatMessageRes::fromEntity);

        Page<Message> messagePage = messageRepository.findByVideoConference(videoConference, pageable);

        Map<UUID, String> memberNameMap = findMemberName(messagePage.map(Message::getSenderId).stream().distinct().toList());

        return messagePage.map((message) -> ChatMessageRes.fromEntity(message, memberNameMap.getOrDefault(message.getSenderId(), "알 수 없는 사용자")));
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

        for(VideoConferenceInvitee invitee : videoConference.getVideoConferenceInviteeSet()) {
            ScheduleDeleteDto scheduleDelete = ScheduleDeleteDto.builder()
                    .originId(invitee.getId())
                    .build();

            eventPublisher.publishEvent(scheduleDelete);
        }
    }

    public MinuteRes findMinute(UUID videoConferenceId) {
        VideoConference videoConference = videoConferenceRepository.findById(videoConferenceId).orElseThrow(() -> new EntityNotFoundException("회의록이 존재하지 않습니다."));

        if (videoConference.getRecording() == null) throw new EntityNotFoundException("회의록이 존재하지 않습니다.");
        if (videoConference.getRecording().getMinute() == null) throw new EntityNotFoundException("회의록이 존재하지 않습니다.");

        return MinuteRes.fromEntity(videoConference);
    }

    public VideoConferencePasswordRes findPassword(UUID memberId, UUID videoConferenceId) {
        VideoConference videoConference = videoConferenceRepository.findById(videoConferenceId).orElseThrow(() -> new EntityNotFoundException("회의록이 존재하지 않습니다."));

        if (videoConference.getStatus() != VideoConferenceStatus.IN_PROGRESS)
            throw new VideoConferenceNotInProgressException("진행 중인 화상회의가 아닙니다.");

        if (videoConference.getVideoConferenceInviteeSet().stream().map(VideoConferenceInvitee::getMemberId).noneMatch(memberId::equals))
            throw new UserNotInvitedException("초대 받지 않은 화상회의입니다.");

        String password = videoConference.getPassword();

        if (password == null) {
            password = UUID.randomUUID().toString();
            videoConference.setPassword(password);
        }

        VideoConferencePasswordRes passwordRes = null;
        try {
            passwordRes = VideoConferencePasswordRes.builder()
                    .id(aesUtil.encrypt(videoConferenceId.toString()))
                    .password(aesUtil.encrypt(password))
                    .build();
        } catch (Exception e) {
            throw new AESUtilException(e.getMessage());
        }

        return passwordRes;
    }

    public LiveKitSessionRes joinVideoConferenceWithPassword(UUID memberId, String memberName, VideoConferenceJoinReq videoConferenceJoinReq) {
        UUID videoConferenceId = null;
        String password = null;
        try {
            videoConferenceId = UUID.fromString(aesUtil.decrypt(videoConferenceJoinReq.getId()));
            password = aesUtil.decrypt(videoConferenceJoinReq.getPassword());
        } catch (Exception e) {
            throw new AESUtilException("화상회의 참여에 실패했습니다.");
        }

        VideoConference videoConference = videoConferenceRepository.findById(videoConferenceId).orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 유효하지 않습니다."));

        if (videoConference.getPassword() == null || !videoConference.getPassword().equals(password))
            throw new IllegalArgumentException("아이디 또는 비밀번호가 유효하지 않습니다.");

        if (videoConference.getStatus() != VideoConferenceStatus.IN_PROGRESS)
            throw new VideoConferenceNotInProgressException("진행 중인 화상회의가 아닙니다.");

        Response<ParticipantInfo> res;
        try {
            res = roomServiceClient.getParticipant(videoConferenceId.toString(), memberId.toString()).execute();
        } catch (IOException e) {
            throw new LiveKitClientException("화상회의 통신 실패" + e.getMessage());
        }
        if (res.isSuccessful()) throw new UserAlreadyJoinedException("이미 참여 중인 화상회의 입니다.");

        videoConference.getVideoConferenceInviteeSet().add(VideoConferenceInvitee.builder().memberId(memberId).videoConference(videoConference).build());
        String token = createToken(videoConferenceId, memberId, memberName);

        return LiveKitSessionRes.builder()
                .videoConferenceId(videoConferenceId)
                .token(token)
                .title(videoConference.getName())
                .build();
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

        LivekitEgress.EncodedFileOutput videoEncodedFileOutput = LivekitEgress.EncodedFileOutput.newBuilder()
                .setFileType(LivekitEgress.EncodedFileType.MP4)
                .setFilepath("recordings/" + videoConference.getId() + "/" + videoConference.getName() + "-composite.mp4")
                .setS3(s3Upload)
                .build();

//        LivekitEgress.EncodedFileOutput audioEncodedFileOutput = LivekitEgress.EncodedFileOutput.newBuilder()
//                .setFileType(LivekitEgress.EncodedFileType.OGG)
//                .setFilepath("recordings/" + videoConference.getId() + "/" + videoConference.getName() + "-composite.ogg")
//                .setS3(s3Upload)
//                .build();

        try {
            egressServiceClient.startRoomCompositeEgress(
                    videoConference.getId().toString(),
                    videoEncodedFileOutput,
                    "speaker"
            ).execute();

//            egressServiceClient.startRoomCompositeEgress(
//                    videoConference.getId().toString(),
//                    audioEncodedFileOutput,
//                    "speaker",
//                    null,
//                    null,
//                    true
//            ).execute();

        } catch (IOException e) {
            log.error(e.getMessage());
            throw new LiveKitClientException(e.getMessage());
        }
    }

    private String createToken(UUID videoConferenceId, UUID participantId, String participantName) {

        AccessToken token = new AccessToken(LIVEKIT_API_KEY, LIVEKIT_API_SECRET);
        token.setIdentity(participantId.toString());
        token.setName(participantName);
        token.addGrants(new RoomJoin(true), new RoomName(videoConferenceId.toString()));
        log.info("Create token for video conference " + participantName);
        return token.toJwt();
    }

    private Map<UUID, String> findMemberName(List<UUID> idList) {
        if (idList.isEmpty()) return Collections.emptyMap();

        IdListReq idListReq = new IdListReq();
        idListReq.setUuidList(idList);

        // member-service 에서 UUID를 하나 보내야하도록 구현되어있어서
        List<MemberNameListRes> memberNameList = memberFeignClient.getNameList(UUID.randomUUID(), idListReq).getData();

        return memberNameList.stream()
                .collect(Collectors.toMap(
                        MemberNameListRes::getMemberId,
                        MemberNameListRes::getName
                ));
    }


    @KafkaListener(
            containerFactory = "meetingKafkaListenerContainerFactory",
            topics = "transcribe-response",
            groupId = "workspace-meeting-group"
    )
    public void listen(String body, Acknowledgment ack) {
        TranscribeRes transcribeRes;
        try {
            transcribeRes = objectMapper.readValue(body, TranscribeRes.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        VideoConference videoConference = videoConferenceRepository.findById(transcribeRes.getVideoConferenceId()).orElseThrow(() -> new EntityNotFoundException("찾을 수 없는 화상회의 입니다."));

        Minute minute = Minute.builder()
                .recording(videoConference.getRecording())
                .status(MinuteStatus.COMPLETED)
                .transcript(transcribeRes.getTranscript())
                .summary(transcribeRes.getSummary())
                .errorMsg("")
                .build();

        minuteRepository.save(minute);

        ack.acknowledge();

        log.info("!!!!! vcs - 회의록 저장 완료 !!!!!!!! 영상 길이 : {}, 소요 시간 : {}", videoConference.getRecording().getDuration(), transcribeRes.getTurnaround());

        MinuteSavedEvent minuteSavedEvent = MinuteSavedEvent.builder()
                .videoConferenceId(videoConference.getId())
                .name(videoConference.getName())
                .summary(transcribeRes.getSummary())
                .hostId(transcribeRes.getSummary())
                .inviteeIdSet(videoConference.getVideoConferenceInviteeSet().stream()
                        .map(vci -> vci.getMemberId().toString()).collect(Collectors.toSet()))
                .createdAt(videoConference.getCreatedAt())
                .build();
        eventPublisher.publishEvent(minuteSavedEvent);
    }

    // 데이터 변경이 완료된 후 Elasticsearch 동기화를 위한 이벤트 저장
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void handleMinuteCompletedEvent(MinuteSavedEvent event) {
        saveSearchOutboxEvent(event);
    }

    // elastic search에 저장
    public void saveSearchOutboxEvent(MinuteSavedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            MinuteSearchOutboxEvent outboxEvent = MinuteSearchOutboxEvent.builder()
                    .topic("minute-completed-events")
                    .aggregateId(event.getVideoConferenceId())
                    .payload(payload)
                    .build();
            minuteSearchOutboxEventRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            throw new SerializationException("데이터 직렬화 실패");
        }
    }
}
