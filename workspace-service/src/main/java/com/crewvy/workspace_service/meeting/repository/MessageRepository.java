package com.crewvy.workspace_service.meeting.repository;

import com.crewvy.workspace_service.meeting.entity.Message;
import com.crewvy.workspace_service.meeting.entity.VideoConference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    Page<Message> findByVideoConference(VideoConference videoConference, Pageable pageable);
}
