package com.jobtracker.apps.api.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobtracker.apps.api.dto.*;
import com.jobtracker.apps.domain.model.Application;
import com.jobtracker.apps.domain.model.ApplicationNote;
import com.jobtracker.apps.domain.model.ApplicationStatusHistory;

import java.io.IOException;
import java.util.List;

public class ApplicationMapper {
    private final ObjectMapper om = new ObjectMapper();

    public ApplicationDto toDto(Application a) {
        String[] tags = null;
        if (a.getTagsJson() != null) {
            try {
                tags = om.readValue(a.getTagsJson(), new TypeReference<>() {});
            } catch (IOException e) {
                // ignore bad tags; keep null
            }
        }
        return new ApplicationDto(
                a.getId(),
                a.getCompany(),
                a.getRole(),
                a.getStatus(),
                a.getSource(),
                a.getLocation(),
                a.getSalaryMin(),
                a.getSalaryMax(),
                a.getCurrency(),
                a.getNextFollowUpOn(),
                tags,
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }

    public NoteDto toDto(ApplicationNote n) {
        return new NoteDto(n.getId(), n.getBody(), n.getCreatedAt());
    }

    public StatusChangeDto toDto(ApplicationStatusHistory h) {
        return new StatusChangeDto(h.getId(), h.getFromStatus(), h.getToStatus(), h.getChangedAt());
    }

    public String tagsToJson(String[] tags) {
        if (tags == null) return null;
        try { return om.writeValueAsString(tags); }
        catch (Exception e) { return null; }
    }
}