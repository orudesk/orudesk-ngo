package com.orudesk.orudesk.donations.adapter.out.organisation;

import com.orudesk.orudesk.donations.application.port.out.OrganisationDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The NGO's own details come from configuration for now (ORUDESK_NGO_NAME...). One NGO per installation is the
 * common case; when several NGOs share one (tenants), these move to a settings table per tenant.
 */
@Configuration
class ConfiguredOrganisation {

    @Bean
    OrganisationDetails organisationDetails(@Value("${orudesk.ngo.name:Orudesk NGO}") String name,
                                            @Value("${orudesk.ngo.address:}") String address,
                                            @Value("${orudesk.ngo.pan:}") String pan,
                                            @Value("${orudesk.ngo.registration-80g:}") String registration80G) {
        return new OrganisationDetails(name, blankToNull(address), blankToNull(pan), blankToNull(registration80G));
    }

    private static String blankToNull(String value) {
        return value.isBlank() ? null : value.strip();
    }
}
