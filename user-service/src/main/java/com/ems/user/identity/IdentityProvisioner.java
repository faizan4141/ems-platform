package com.ems.user.identity;

import java.util.UUID;

public interface IdentityProvisioner {

    /**
     * Creates a login (with tenant_id and role) and returns its identity-provider id.
     */
    UUID createUser(NewLogin login);

    /**
     * Emails the user a link to set their password.
     */
    void sendSetPasswordEmail(UUID identityUserId);

    void deleteUser(UUID identityUserId);
}