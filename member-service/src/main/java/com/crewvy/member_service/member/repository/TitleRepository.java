package com.crewvy.member_service.member.repository;

import com.crewvy.member_service.member.entity.Company;
import com.crewvy.member_service.member.entity.Title;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TitleRepository extends JpaRepository<Title, UUID> {
    List<Title> findAllByCompany(Company company);
    List<Title> findAllByCompanyOrderByDisplayOrderAsc(Company company); // displayOrder로 정렬 추가
}
