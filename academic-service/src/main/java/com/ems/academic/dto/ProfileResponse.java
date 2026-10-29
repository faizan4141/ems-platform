package com.ems.academic.dto;

import com.ems.academic.domain.SchoolProfile;

public record ProfileResponse(boolean configured, String displayName, String board, String affiliationNo,
                              String addressLine, String city, String state, String postalCode,
                              String phone, String email, String website) {

    public static ProfileResponse from(SchoolProfile p) {
        return new ProfileResponse(true, p.getDisplayName(), p.getBoard(), p.getAffiliationNo(),
                p.getAddressLine(), p.getCity(), p.getState(), p.getPostalCode(),
                p.getPhone(), p.getEmail(), p.getWebsite());
    }

    /** Before the school has saved a profile, pre-fill the name from the platform record. */
    public static ProfileResponse unconfigured(String name) {
        return new ProfileResponse(false, name, null, null, null, null, null, null, null, null, null);
    }
}