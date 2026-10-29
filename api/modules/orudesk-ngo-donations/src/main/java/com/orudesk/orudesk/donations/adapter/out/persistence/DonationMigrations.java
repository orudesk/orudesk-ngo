package com.orudesk.orudesk.donations.adapter.out.persistence;

import com.orudesk.orudesk.shared.ModuleMigrations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/** ngo_donation points at ngo_donor, so donations' migrations run after the donors module's (@Order 0). */
@Configuration
class DonationMigrations {

    @Bean
    @Order(1)
    ModuleMigrations donationsModuleMigrations() {
        return new ModuleMigrations("donations", "classpath:db/migration/donations");
    }
}
