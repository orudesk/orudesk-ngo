package com.orudesk.orudesk.donors.adapter.out.persistence;

import com.orudesk.orudesk.shared.ModuleMigrations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** ngo_donor points at party_person. No @Order: a vertical's modules run after the core's. */
@Configuration
class DonorMigrations {

    @Bean
    ModuleMigrations donorsModuleMigrations() {
        return new ModuleMigrations("donors", "classpath:db/migration/donors");
    }
}
