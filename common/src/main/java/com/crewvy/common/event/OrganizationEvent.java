package com.crewvy.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID id;
    private String name;
    private UUID parentId;

}
