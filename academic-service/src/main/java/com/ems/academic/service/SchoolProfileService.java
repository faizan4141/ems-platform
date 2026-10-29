package com.ems.academic.service;

import com.ems.academic.domain.SchoolProfile;
import com.ems.academic.dto.ProfileRequest;
import com.ems.academic.dto.ProfileResponse;
import com.ems.academic.repository.SchoolProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SchoolProfileService {

    private final SchoolProfileRepository profiles;
    private final TenantGuard guard;

    public SchoolProfileService(SchoolProfileRepository profiles, TenantGuard guard) {
        this.profiles = profiles;
        this.guard = guard;
    }

    @Transactional(readOnly = true)
    public ProfileResponse get(UUID tenantId) {
        String platformName = guard.requireActive(tenantId).getName();
        return profiles.findById(tenantId)
                .map(ProfileResponse::from)
                .orElseGet(() -> ProfileResponse.unconfigured(platformName));
    }

    @Transactional
    public ProfileResponse save(UUID tenantId, ProfileRequest request) {
        guard.requireActive(tenantId);
        SchoolProfile profile = profiles.findById(tenantId).orElseGet(() -> new SchoolProfile(tenantId));
        profile.apply(request);
        return ProfileResponse.from(profiles.save(profile));
    }
}