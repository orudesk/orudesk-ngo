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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
                .andExpect(jsonPath("$.receivesUpdates").value(true))
                .andExpect(jsonPath("$.dateOfBirth").value("1980-05-01"));   // the edit form sends it back
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

    @Test
    void aDonorManagerCorrectsADonorAndThePanIsAuditedWithoutItsValue() throws Exception {
        String token = donorManager();
        String id = registerDonor(token, "Meera", "ABCDE1234F");

        edit(token, id, 0, 0, "+91 98450 99999", """
                "type": "CORPORATE", "pan": "PQRST6789Z", "wantsTaxReceipt": true, "receivesUpdates": true""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("+91 98450 99999"))
                .andExpect(jsonPath("$.type").value("CORPORATE"))
                .andExpect(jsonPath("$.panMasked").value("XXXXXX789Z"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.personVersion").value(1));

        mockMvc.perform(get("/api/v1/audit-entries").param("subjectId", id)
                        .header("Authorization", "Bearer " + login("test-admin@orudesk.org")))
                .andExpect(jsonPath("$.items[0].summary").value("Changed type, pan, wantsTaxReceipt of donor Meera Test-Iyer"))
                .andExpect(jsonPath("$.items[0].changes[1].field").value("pan"))
                .andExpect(jsonPath("$.items[0].changes[1].valuesHidden").value(true))
                .andExpect(jsonPath("$.items[0].changes[1].before").doesNotExist())
                .andExpect(jsonPath("$.items[0].changes[1].after").doesNotExist());
    }

    @Test
    void leavingThePanOutKeepsItAndABlankRemovesIt() throws Exception {
        String token = donorManager();
        String id = registerDonor(token, "Meera", "ABCDE1234F");

        edit(token, id, 0, 0, "+91 98450 12345", """
                "receivesUpdates": false""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.panMasked").value("XXXXXX234F"))
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.personVersion").value(0));   // nothing of the person changed: no write

        edit(token, id, 1, 0, "+91 98450 12345", """
                "pan": "", "wantsTaxReceipt": true""")
                .andExpect(status().isBadRequest());                  // a receipt needs a PAN, as at registration
        edit(token, id, 1, 0, "+91 98450 12345", "\"pan\": \"\"")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.panMasked").doesNotExist());
    }

    @Test
    void anEditOnAStaleVersionChangesNeitherRecord() throws Exception {
        String token = donorManager();
        String id = registerDonor(token, "Meera", "ABCDE1234F");
        edit(token, id, 0, 0, "+91 98450 99999", """
                "receivesUpdates": true""").andExpect(status().isOk());   // the person moves to 1

        edit(token, id, 0, 0, "+91 98450 00000", """
                "type": "FOUNDATION", "receivesUpdates": true""")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.currentVersion").value(1));

        mockMvc.perform(get("/api/v1/donors/{id}", id).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.type").value("INDIVIDUAL"))
                .andExpect(jsonPath("$.phone").value("+91 98450 99999"));
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

    /** @param donorFields the donor's part of the form, as JSON members */
    private ResultActions edit(String token, String id, int version, int personVersion, String phone,
                               String donorFields) throws Exception {
        return mockMvc.perform(put("/api/v1/donors/{id}", id).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"version": %d, "personVersion": %d, "firstName": "Meera", "lastName": "Test-Iyer",
                         "dateOfBirth": "1980-05-01", "phone": "%s", %s}
                        """.formatted(version, personVersion, phone, donorFields)));
    }

    private String registerDonor(String token, String firstName, String pan) throws Exception {
        String json = mockMvc.perform(post("/api/v1/donors").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "%s", "lastName": "Test-Iyer", "phone": "+91 98450 12345",
                                 "dateOfBirth": "1980-05-01", "pan": "%s", "receivesUpdates": true}
                                """.formatted(firstName, pan)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }
}
