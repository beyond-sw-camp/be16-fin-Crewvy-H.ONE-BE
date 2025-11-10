package com.crewvy.member_service.member.repository;

import com.crewvy.member_service.member.entity.Company;
import com.crewvy.member_service.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemberRepository extends JpaRepository<Member, UUID> {
    @Query("SELECT m FROM Member m LEFT JOIN FETCH m.defaultMemberPosition WHERE m.email = :email")
    Optional<Member> findByEmail(@Param("email") String email);

    @Query("SELECT m FROM Member m " +
           "LEFT JOIN FETCH m.defaultMemberPosition mp " +
           "LEFT JOIN FETCH mp.title " +
           "LEFT JOIN FETCH mp.organization " +
           "WHERE m.company = :company")
    List<Member> findByCompanyWithDetail(@Param("company") Company company);

    @Query("select m from Member m left join fetch m.memberPositionList mpl where m.id = :memberId")
    Optional<Member> findByIdWithDetail(@Param("memberId") UUID memberId);

    List<Member> findByDefaultMemberPosition_Id(UUID memberPositionId);

    @Query("SELECT COUNT(DISTINCT mp.member) FROM MemberPosition mp WHERE mp.organization.id = :organizationId")
    long countByOrganizationId(@Param("organizationId") UUID organizationId);

    @Query("SELECT m FROM Member m WHERE m.company.id = :companyId")
    List<Member> findByCompanyId(@Param("companyId") UUID companyId);
}
