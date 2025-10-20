package com.crewvy.workspace_service.meeting.repository;

import com.crewvy.workspace_service.meeting.entity.Recording;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecordingRepository extends JpaRepository<Recording, Integer> {
}
