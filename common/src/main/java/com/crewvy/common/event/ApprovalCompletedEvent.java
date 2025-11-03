package com.crewvy.common.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalCompletedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID approvalId;
    private UUID memberId;
    private UUID companyId;
    private String title;
    private String memberName;
    private String titleName;
    @JsonProperty("createDateTime")
    private LocalDateTime createdAt;
}
