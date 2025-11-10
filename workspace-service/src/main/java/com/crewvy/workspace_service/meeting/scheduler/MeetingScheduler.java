package com.crewvy.workspace_service.meeting.scheduler;

import com.crewvy.common.dto.NotificationMessage;
import com.crewvy.workspace_service.meeting.constant.VideoConferenceStatus;
import com.crewvy.workspace_service.meeting.repository.VideoConferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
public class MeetingScheduler {
    private final VideoConferenceRepository videoConferenceRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(cron = "0 0 0 * * *")
    @Async
    public void deletePastScheduledMeetings() {
        videoConferenceRepository.deleteByScheduledStartTimeBeforeAndStatus(LocalDateTime.now(), VideoConferenceStatus.WAITING);
    }

    @Scheduled(cron = "0 0,15,30,45 * * * *")
    @Async
    public void sendScheduledMeetingNotifications() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime after15Minutes = now.plusMinutes(1);
        videoConferenceRepository.findWithInviteesByScheduledStartTimeBetweenAndStatus(now, after15Minutes, VideoConferenceStatus.WAITING)
                .forEach(videoConference -> videoConference.getVideoConferenceInviteeSet().forEach(invitee -> {
                    NotificationMessage message = NotificationMessage.builder()
                            .memberId(invitee.getMemberId())
                            .targetId(videoConference.getId())
                            .notificationType("NT002")
                            .content("화상회의 : 잠시 후 (" + videoConference.getScheduledStartTime().toString() +") " + videoConference.getName() + "가 예정되어 있습니다.")
                                    .build();
                    eventPublisher.publishEvent(message);
                }));
    }
}
