package com.crewvy.common.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MinuteSavedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID videoConferenceId;
    private String name;
    private String summary;
    private String hostId;
    private Set<String> inviteeIdSet = new HashSet<>();
    @JsonProperty("createDateTime")
    private LocalDateTime createdAt;
}
