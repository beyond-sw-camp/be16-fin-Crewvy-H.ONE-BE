package com.crewvy.member_service.member.entity;

import com.crewvy.common.entity.BaseEntity;
import com.crewvy.common.entity.Bool;
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
public class Grade extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT), nullable = false)
    private Company company;

    @Column(nullable = false)
    @Builder.Default
    private Integer displayOrder = 0; // displayOrder 필드 추가

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Bool ynDel = Bool.FALSE;

    public void updateName(String name) {
        this.name = name;
    }

    public void updateDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public void delete() {
        this.ynDel = Bool.TRUE;
    }

    public void restore() {
        this.ynDel = Bool.FALSE;
    }
}
