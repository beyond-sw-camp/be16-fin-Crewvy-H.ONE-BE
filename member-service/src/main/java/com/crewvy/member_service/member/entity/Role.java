package com.crewvy.member_service.member.entity;

import com.crewvy.common.entity.BaseEntity;
import com.crewvy.common.entity.Bool;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Bool ynDel = Bool.TRUE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT), nullable = false)
    private Company company;

    @Builder.Default
    @OneToMany(mappedBy = "role", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    private List<RolePermission> rolePermissionList = new ArrayList<>();

    public void updatePermission(List<RolePermission> newRolePermissions){
        this.rolePermissionList.clear();
        if (newRolePermissions != null) {
            this.rolePermissionList.addAll(newRolePermissions);
        }
    }

    public void updateName(String newName){
        this.name = newName;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void delete() {
        this.ynDel = Bool.FALSE;
    }
}