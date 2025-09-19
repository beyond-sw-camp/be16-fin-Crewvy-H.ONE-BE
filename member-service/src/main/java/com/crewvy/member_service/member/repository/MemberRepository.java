package com.crewvy.member_service.member.repository;

import com.chillex.gooseBumps.common.constant.code.member.Role;
import com.chillex.gooseBumps.common.constant.code.member.SocialType;
import com.chillex.gooseBumps.domain.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Page<Member> findAll(Specification<Member> specification, Pageable pageable);
    Optional<Member> findByEmail(String email);
    Optional<Member> findBySocialId(String socialId);
    Optional<Member> findByEmailAndSocialType(String email, SocialType socialType);;
    @Query("SELECT m FROM Member m WHERE (m.nickName LIKE %:keyword% OR m.email LIKE %:keyword% OR m.name LIKE %:keyword%) " +
            "AND m.ynDel = :ynDel AND m.roleCode = :role AND m.memberSeq != :memberSeq ORDER BY m.createdAt")
    List<Member> findAllBySearchKeyword(@Param("keyword") String keyword, @Param("ynDel") String ynDel, @Param("role") Role role,
                                        @Param("memberSeq") long memberSeq);

}
