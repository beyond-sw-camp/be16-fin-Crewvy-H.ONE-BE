package com.crewvy.workforce_service.performance.converter;

import com.crewvy.workforce_service.performance.constant.TeamGoalStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class TeamGoalStatusConverter implements AttributeConverter<TeamGoalStatus, String> {

    @Override
    public String convertToDatabaseColumn(TeamGoalStatus teamGoalStatus) {
        if (teamGoalStatus == null) {
            return null;
        }
        return teamGoalStatus.getCodeValue();
    }

    @Override
    public TeamGoalStatus convertToEntityAttribute(String codeValue) {
        if (codeValue == null) {
            return null;
        }
        return TeamGoalStatus.fromCode(codeValue);
    }
}
