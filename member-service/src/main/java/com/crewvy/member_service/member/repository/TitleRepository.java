package com.crewvy.member_service.member.repository;

import com.crewvy.common.entity.Bool;
import com.crewvy.member_service.member.entity.Company;
import com.crewvy.member_service.member.entity.Title;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TitleRepository extends JpaRepository<Title, UUID> {
    List<Title> findAllByCompanyAndYnDelOrderByDisplayOrderAsc(Company company, Bool ynDel);
    List<Title> findAllByCompanyOrderByDisplayOrderAsc(Company company);
}
