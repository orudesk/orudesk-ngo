package com.orudesk.orudesk.donors.adapter.out.persistence;

import com.orudesk.orudesk.shared.ModuleMigrations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * ngo_donor points at party_person: the core's modules run first (their @Order is HIGHEST_PRECEDENCE + n).
 * @Order(0): before donations (1), whose table points at this one (Day 56).
 */
@Configuration
class DonorMigrations {

    @Bean
    @Order(0)
    ModuleMigrations donorsModuleMigrations() {
        return new ModuleMigrations("donors", "classpath:db/migration/donors");
    }
}
