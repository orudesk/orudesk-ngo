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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Donors through the whole NGO app, using only the core's public HTTP API for logins and roles: the way a
 * vertical written by someone else would test, without reaching into identity's classes.
 */
@SpringBootTest(properties = {
        "orudesk.identity.first-admin.email=test-admin@orudesk.org",
        "orudesk.identity.first-admin.password=admin-password"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DonorsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM ngo_donor");
        jdbc.update("DELETE FROM party_person WHERE last_name = 'Test-Iyer'");
        jdbc.update("DELETE FROM iam_user WHERE email LIKE 'test-user-%@orudesk.org'");
    }

    @Test
    void theDonorManagerRoleExistsAtStartup() {
        assertThat(jdbc.queryForObject("SELECT name FROM iam_role WHERE code = 'DONOR_MANAGER'", String.class))
                .isEqualTo("Donor manager");
    }

    @Test
    void aDonorManagerRegistersADonorAndReadsItBack() throws Exception {
        String token = donorManager();
        String id = registerDonor(token, "Meera", "abcde1234f");

        mockMvc.perform(get("/api/v1/donors/{id}", id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Meera"))      // from the People registry
                .andExpect(jsonPath("$.type").value("INDIVIDUAL"))                // the default
                .andExpect(jsonPath("$.panMasked").value("XXXXXX234F"))
                .andExpect(jsonPath("$.receivesUpdates").value(true));
    }

    @Test
    void theDonorIsAlsoInThePeopleRegistry() throws Exception {
        registerDonor(login("test-admin@orudesk.org"), "Meera", "ABCDE1234F");

        assertThat(jdbc.queryForObject("""
                SELECT p.first_name FROM ngo_donor d JOIN party_person p ON p.id = d.person_id
                WHERE p.last_name = 'Test-Iyer'
                """, String.class)).isEqualTo("Meera");
    }

    @Test
    void aSecondDonorWithTheSamePanIsRefusedAndLeavesNoPersonBehind() throws Exception {
        String token = donorManager();
        registerDonor(token, "Meera", "ABCDE1234F");

        mockMvc.perform(post("/api/v1/donors").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "Meera K.", "lastName": "Test-Iyer", "pan": "ABCDE1234F"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("PAN already registered"));

        // The person was inserted first, then the donor failed: one transaction, so both rolled back.
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM party_person WHERE last_name = 'Test-Iyer'",
                Integer.class)).isEqualTo(1);
    }

    @Test
    void anOrdinaryUserMayNotSeeDonors() throws Exception {
        register("test-user-plain@orudesk.org");

        mockMvc.perform(post("/api/v1/donors").header("Authorization", "Bearer " + login("test-user-plain@orudesk.org"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "Meera", "lastName": "Test-Iyer"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void aBadPanIsRefusedBeforeAnythingIsSaved() throws Exception {
        mockMvc.perform(post("/api/v1/donors").header("Authorization", "Bearer " + donorManager())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "Meera", "lastName": "Test-Iyer", "pan": "1234"}
                                """))
                .andExpect(status().isBadRequest());
    }

    /** A new user, given DONOR_MANAGER by the admin through the core's API, then logged in. */
    private String donorManager() throws Exception {
        String userId = register("test-user-donors@orudesk.org");
        mockMvc.perform(post("/api/v1/users/{id}/roles", userId)
                        .header("Authorization", "Bearer " + login("test-admin@orudesk.org"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role": "DONOR_MANAGER"}
                                """))
                .andExpect(status().is2xxSuccessful());
        return login("test-user-donors@orudesk.org");   // a new token, which carries the new role
    }

    private String register(String email) throws Exception {
        String json = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "user-password"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    private String login(String email) throws Exception {
        String password = email.equals("test-admin@orudesk.org") ? "admin-password" : "user-password";
        String json = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.accessToken");
    }

    private String registerDonor(String token, String firstName, String pan) throws Exception {
        String json = mockMvc.perform(post("/api/v1/donors").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "%s", "lastName": "Test-Iyer", "phone": "+91 98450 12345",
                                 "pan": "%s", "receivesUpdates": true}
                                """.formatted(firstName, pan)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }
}
