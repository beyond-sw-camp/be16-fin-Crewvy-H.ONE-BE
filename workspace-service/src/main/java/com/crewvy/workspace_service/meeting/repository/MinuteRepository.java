package com.crewvy.workspace_service.meeting.repository;

import com.crewvy.workspace_service.meeting.entity.Minute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MinuteRepository extends JpaRepository<Minute, UUID> {
}
