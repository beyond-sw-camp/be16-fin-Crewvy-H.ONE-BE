package com.crewvy.workspace_service.calendar.entity;

import com.crewvy.common.entity.Bool;
import com.crewvy.workspace_service.calendar.constant.CalendarType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@Entity
public class Calendar {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID memberId;

    private String title;

    private String contents;

    private CalendarType type;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private UUID originId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "is_deleted", nullable = false)
    private Bool isDeleted = Bool.FALSE;

    public void updateSchedule(String title, String contents, LocalDateTime startDate, LocalDateTime endDate) {
        this.title = title;
        this.contents = contents;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void deleteSchedule() {
        this.isDeleted = Bool.TRUE;
    }
}
