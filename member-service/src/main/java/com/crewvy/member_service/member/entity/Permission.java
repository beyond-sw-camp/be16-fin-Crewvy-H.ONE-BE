package com.crewvy.member_service.member.entity;

import com.crewvy.common.entity.BaseEntity;
import com.crewvy.member_service.member.constant.Action;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String resource;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Action action;

    private String description;
}
