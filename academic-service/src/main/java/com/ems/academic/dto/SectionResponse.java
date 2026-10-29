package com.ems.academic.dto;

import com.ems.academic.domain.Section;
import java.util.UUID;

public record SectionResponse(UUID id, String name, Integer capacity) {

    public static SectionResponse from(Section s) {
        return new SectionResponse(s.getId(), s.getName(), s.getCapacity());
    }
}