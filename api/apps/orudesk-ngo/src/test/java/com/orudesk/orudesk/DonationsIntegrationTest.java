package com.orudesk.orudesk;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Day 56: donations through the whole NGO app, against PostgreSQL. */
@SpringBootTest(properties = {
        "orudesk.identity.first-admin.email=test-admin@orudesk.org",
        "orudesk.identity.first-admin.password=admin-password"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DonationsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM ngo_donation");
        jdbc.update("DELETE FROM ngo_donor");
        jdbc.update("DELETE FROM party_person WHERE last_name = 'Test-Gift'");
    }

    @Test
    void aDonationIsRecordedExactlyAndAddsUpOnTheDonorsPage() throws Exception {
        String token = admin();
        String donor = donor(token);

        String id = recordDonation(token, donor, "\"1500.10\"", "UPI", "\"UTR-1\"").andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        recordDonation(token, donor, "\"0.20\"", "CASH", "null").andExpect(status().isCreated());

        mockMvc.perform(get(id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value("1500.10"))
                .andExpect(jsonPath("$.amountFormatted").value("₹1,500.10"))
                .andExpect(jsonPath("$.donorName").value("Asha Test-Gift"))
                .andExpect(jsonPath("$.mode").value("UPI"));
        mockMvc.perform(get("/api/v1/donors/{id}/donations", donor).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.donations.length()").value(2))
                .andExpect(jsonPath("$.total").value("1500.30"));   // not 1500.3000000000002
    }

    @Test
    void theRulesHoldInTheApiAndInTheDatabase() throws Exception {
        String token = admin();
        String donor = donor(token);

        recordDonation(token, donor, "\"200000\"", "CASH", "null")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("269ST")));
        recordDonation(token, donor, "\"10.005\"", "CASH", "null").andExpect(status().isBadRequest());

        // Whoever writes the row, the database refuses the same cash
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO ngo_donation (id, tenant_id, donor_id, amount, received_on, mode)
                VALUES (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', ?, 250000, CURRENT_DATE, 'CASH')
                """, UUID.fromString(donor))).hasMessageContaining("ck_ngo_donation_cash_limit");
    }

    @Test
    void anUnknownDonorIsNotFound() throws Exception {
        recordDonation(admin(), UUID.randomUUID().toString(), "\"100\"", "CASH", "null")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Donor not found"));
    }

    @Test
    void anOrdinaryUserCannotSeeDonations() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"test-user-gift@orudesk.org\", \"password\": \"user-password\"}"));
        String token = login("test-user-gift@orudesk.org", "user-password");
        mockMvc.perform(get("/api/v1/donors/{id}/donations", UUID.randomUUID()).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        jdbc.update("DELETE FROM iam_user WHERE email = 'test-user-gift@orudesk.org'");
    }

    private ResultActions recordDonation(String token, String donorId, String amount, String mode, String reference)
            throws Exception {
        return mockMvc.perform(post("/api/v1/donations").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"donorId": "%s", "amount": %s, "receivedOn": "%s", "mode": "%s", "reference": %s}
                        """.formatted(donorId, amount, LocalDate.now(), mode, reference)));
    }

    private String donor(String token) throws Exception {
        String json = mockMvc.perform(post("/api/v1/donors").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "Asha", "lastName": "Test-Gift", "email": "asha@example.org",
                                 "pan": "ABCDE1234F", "wantsTaxReceipt": true}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    private String admin() throws Exception {
        return login("test-admin@orudesk.org", "admin-password");
    }

    private String login(String email, String password) throws Exception {
        String json = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.accessToken");
    }
}
