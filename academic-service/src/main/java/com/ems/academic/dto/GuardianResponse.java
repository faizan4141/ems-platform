package com.ems.academic.dto;

import com.ems.academic.domain.Guardian;
import com.ems.academic.domain.Relationship;
import java.util.UUID;

public record GuardianResponse(UUID id, String name, Relationship relationship, String phone,
                               String email, boolean primary, boolean portalAccess) {

    public static GuardianResponse from(Guardian g, boolean portalAccess) {
        return new GuardianResponse(g.getId(), g.getName(), g.getRelationship(), g.getPhone(),
                g.getEmail(), g.isPrimary(), portalAccess);
    }
}