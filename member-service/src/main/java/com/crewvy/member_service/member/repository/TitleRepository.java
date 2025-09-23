package com.crewvy.member_service.member.repository;

import com.crewvy.member_service.member.entity.Title;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TitleRepository extends JpaRepository<Title, UUID> {
}
