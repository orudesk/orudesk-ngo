package com.orudesk.orudesk.donors.adapter.in.web;

import com.orudesk.orudesk.identity.RoleDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The role this module needs. Identity adds it to the role list at startup (see RoleDefinition). */
@Configuration
class DonorRoles {

    static final String DONOR_MANAGER = "DONOR_MANAGER";

    @Bean
    RoleDefinition donorManagerRole() {
        return new RoleDefinition(DONOR_MANAGER, "Donor manager", "Registers donors and sees their details");
    }
}
