package com.crewvy.workspace_service.meeting.repository;

import com.crewvy.workspace_service.meeting.constant.VideoConferenceStatus;
import com.crewvy.workspace_service.meeting.entity.VideoConference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface VideoConferenceRepository extends JpaRepository<VideoConference, UUID> {
    Page<VideoConference> findByVideoConferenceInviteeSet_MemberIdAndStatus(UUID memberId, VideoConferenceStatus status, Pageable pageable);

    List<VideoConference> findByVideoConferenceInviteeSet_MemberIdAndStatus(UUID memberId, VideoConferenceStatus status);

    @Query("select distinct vc from VideoConference vc " +
            "join fetch vc.videoConferenceInviteeSet " +
            "where vc.id in (" +
            "select v.id from VideoConference v " +
            "join v.videoConferenceInviteeSet i " +
            "where i.memberId = :memberId and v.status = :status)")
    Page<VideoConference> findByVideoConferenceInviteeList_MemberIdAndStatusFetchInvitees(
            @Param("memberId") UUID memberId,
            @Param("status") VideoConferenceStatus status,
            Pageable pageable);

    @Query("select distinct vc from VideoConference vc " +
            "join fetch vc.videoConferenceInviteeSet " +
            "where vc.id in (" +
            "select v.id from VideoConference v " +
            "join v.videoConferenceInviteeSet i " +
            "where i.memberId = :memberId and v.status = :status)")
    List<VideoConference> findByVideoConferenceInviteeList_MemberIdAndStatusFetchInvitees(
            @Param("memberId") UUID memberId,
            @Param("status") VideoConferenceStatus status);

    void deleteByScheduledStartTimeBeforeAndStatus(LocalDateTime now, VideoConferenceStatus status);

    List<VideoConference> findByScheduledStartTimeBetweenAndStatus(LocalDateTime start, LocalDateTime end, VideoConferenceStatus status);

    @Query("select v from VideoConference v " +
            "join fetch v.videoConferenceInviteeSet " +
            "where v.scheduledStartTime between :start and :end and v.status = :status")
    List<VideoConference> findWithInviteesByScheduledStartTimeBetweenAndStatus(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("status") VideoConferenceStatus status);

}
