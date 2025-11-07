package com.crewvy.common.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PositionNameChangedEvent {
    private String oldPositionName;
    private String newPositionName;
    private String companyId;
}
