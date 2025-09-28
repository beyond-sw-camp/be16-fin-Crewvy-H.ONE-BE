package com.crewvy.workspace_service.meeting.converter;

import com.crewvy.workspace_service.meeting.constant.VideoConferenceStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class VideoConferenceStatusConverter implements AttributeConverter<VideoConferenceStatus, String> {
    @Override
    public String convertToDatabaseColumn(VideoConferenceStatus attribute) {
        return attribute != null ? attribute.getCodeValue() : null;
    }

    @Override
    public VideoConferenceStatus convertToEntityAttribute(String dbData) {
        return dbData != null ? VideoConferenceStatus.fromCode(dbData) : null;
    }
}
