package com.crewvy.workspace_service.meeting.service;

import com.crewvy.workspace_service.meeting.constant.VideoConferenceStatus;
import com.crewvy.workspace_service.meeting.dto.VideoConferenceBookRes;
import com.crewvy.workspace_service.meeting.dto.VideoConferenceCreateReq;
import com.crewvy.workspace_service.meeting.dto.VideoConferenceCreateRes;
import com.crewvy.workspace_service.meeting.dto.VideoConferenceListRes;
import com.crewvy.workspace_service.meeting.entity.VideoConference;
import com.crewvy.workspace_service.meeting.entity.VideoConferenceInvitee;
import com.crewvy.workspace_service.meeting.repository.VideoConferenceRepository;
import io.openvidu.java.client.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

// TODO : jwt가 이메일에서 UUID으로 변경되면 더미가 아니라 진짜 요청한 유저의 UUID를 넣어야 함
//  new UUID(123, 123) -> 진짜 UUID

@Service
@Transactional
public class VideoConferenceService {
    private final VideoConferenceRepository videoConferenceRepository;
    private final OpenVidu openVidu;

    public VideoConferenceService(VideoConferenceRepository videoConferenceRepository, OpenVidu openVidu) {
        this.videoConferenceRepository = videoConferenceRepository;
        this.openVidu = openVidu;
    }

    public VideoConferenceCreateRes createVideoConference(VideoConferenceCreateReq videoConferenceCreateReq) {
        VideoConference videoConference = videoConferenceCreateReq.toEntity(new UUID(123, 123));

        videoConferenceCreateReq.getInviteeIdList().add(new UUID(123, 123));
        addInvitee(videoConference, videoConferenceCreateReq.getInviteeIdList());

        Session session;
        String token;
        try {
            session = openVidu.createSession();
            ConnectionProperties connectionProperties = new ConnectionProperties.Builder()
                    .type(ConnectionType.WEBRTC)
                    .build();
            token = session.createConnection(connectionProperties).getToken();
            videoConference.startVideoConference(session.getSessionId());
        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            throw new RuntimeException("Error creating session", e);
        }
        videoConferenceRepository.save(videoConference);

        return VideoConferenceCreateRes.builder().sessionId(session.getSessionId()).token(token).build();
    }

    public VideoConferenceBookRes bookVideoConference(VideoConferenceCreateReq videoConferenceCreateReq) {
        VideoConference videoConference = videoConferenceCreateReq.toEntity(new UUID(123, 123));

        videoConferenceCreateReq.getInviteeIdList().add(new UUID(123, 123));
        addInvitee(videoConference, videoConferenceCreateReq.getInviteeIdList());

        videoConferenceRepository.save(videoConference);

        return VideoConferenceBookRes.fromEntity(videoConference);
    }

    @Transactional(readOnly = true)
    public Page<VideoConferenceListRes> findAllMyVideoConference(UUID memberId, VideoConferenceStatus videoConferenceStatus, Pageable pageable) {
        return videoConferenceRepository.findByVideoConferenceInviteeList_MemberIdAndStatus(new UUID(123, 123), videoConferenceStatus, pageable)
                .map(VideoConferenceListRes::fromEntity);
    }

    private void addInvitee(VideoConference videoConference, List<UUID> inviteeIdList) {
        inviteeIdList.stream()
                .map(id -> VideoConferenceInvitee.builder().memberId(id).videoConference(videoConference).build())
                .forEach(videoConference.getVideoConferenceInviteeList()::add);
    }
}
