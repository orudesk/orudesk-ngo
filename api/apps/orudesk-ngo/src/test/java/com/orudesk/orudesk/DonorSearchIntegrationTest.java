package com.orudesk.orudesk;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The donor list, search and counts, all read from the v_ngo_donor_summary view. */
@SpringBootTest(properties = {
        "orudesk.identity.first-admin.email=test-admin@orudesk.org",
        "orudesk.identity.first-admin.password=admin-password"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DonorSearchIntegrationTest {

    private static final UUID ACME = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private String admin;

    @BeforeEach
    void someDonors() throws Exception {
        admin = login("test-admin@orudesk.org", "admin-password");
        registerDonor(admin, "Chitra", "INDIVIDUAL", "AAAAA1111A", true);
        registerDonor(admin, "Asha", "INDIVIDUAL", null, false);
        registerDonor(admin, "Bina", "CORPORATE", "BBBBB2222B", true);
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM ngo_donor");
        jdbc.update("DELETE FROM party_person WHERE last_name = 'Test-Search'");
        jdbc.update("DELETE FROM iam_user WHERE email LIKE 'test-user-%@orudesk.org'");
        jdbc.update("DELETE FROM iam_tenant WHERE id = ?", ACME);
    }

    @Test
    void pagesThroughDonorsInNameOrder() throws Exception {
        String page1 = mockMvc.perform(get("/api/v1/donors").param("q", "test-search").param("size", "2")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].firstName").value(contains("Asha", "Bina")))
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(get("/api/v1/donors").param("q", "test-search").param("size", "2")
                        .param("after", JsonPath.<String>read(page1, "$.nextCursor"))
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].firstName").value(contains("Chitra")))
                .andExpect(jsonPath("$.nextCursor").value(nullValue()));
    }

    @Test
    void filtersByType() throws Exception {
        mockMvc.perform(get("/api/v1/donors").param("type", "CORPORATE").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].firstName").value(contains("Bina")));
    }

    @Test
    void countsByTypeInTheDatabase() throws Exception {
        mockMvc.perform(get("/api/v1/donors/counts").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("INDIVIDUAL"))
                .andExpect(jsonPath("$[0].donors").value(2))
                .andExpect(jsonPath("$[0].wantingTaxReceipts").value(1))
                .andExpect(jsonPath("$[1].type").value("CORPORATE"))
                .andExpect(jsonPath("$[1].donors").value(1));
    }

    @Test
    void anotherTenantSeesNoneOfThem() throws Exception {
        jdbc.update("INSERT INTO iam_tenant (id, code, name) VALUES (?, 'acme', 'Acme Trust')", ACME);
        String userId = register("test-user-acme@orudesk.org");
        assignRole(userId, "DONOR_MANAGER");
        jdbc.update("UPDATE iam_user SET tenant_id = ? WHERE id = ?", ACME, UUID.fromString(userId));
        String acme = login("test-user-acme@orudesk.org", "user-password");

        mockMvc.perform(get("/api/v1/donors").header("Authorization", "Bearer " + acme))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
        mockMvc.perform(get("/api/v1/donors/counts").header("Authorization", "Bearer " + acme))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void aTaxReceiptWithoutAPanIsRefused() throws Exception {
        mockMvc.perform(post("/api/v1/donors").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "Deepa", "lastName": "Test-Search", "wantsTaxReceipt": true}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("a tax receipt needs a PAN"));
    }

    @Test
    void theDatabaseRefusesItToo() {
        UUID person = jdbc.queryForObject("SELECT person_id FROM ngo_donor LIMIT 1", UUID.class);

        assertThatThrownBy(() -> jdbc.update("UPDATE ngo_donor SET pan = NULL WHERE person_id = ? AND wants_tax_receipt",
                person)).isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_ngo_donor_receipt_needs_pan");
    }

    @Test
    void theViewNeverShowsTheFullPan() {
        assertThat(jdbc.queryForList("""
                SELECT column_name FROM information_schema.columns WHERE table_name = 'v_ngo_donor_summary'
                """, String.class)).contains("pan_masked").doesNotContain("pan");
    }

    private void registerDonor(String token, String firstName, String type, String pan, boolean taxReceipt)
            throws Exception {
        mockMvc.perform(post("/api/v1/donors").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "%s", "lastName": "Test-Search", "type": "%s", "pan": %s,
                                 "wantsTaxReceipt": %s}
                                """.formatted(firstName, type, pan == null ? "null" : "\"" + pan + "\"", taxReceipt)))
                .andExpect(status().isCreated());
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

    private void assignRole(String userId, String role) throws Exception {
        mockMvc.perform(post("/api/v1/users/{id}/roles", userId).header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role": "%s"}
                                """.formatted(role)))
                .andExpect(status().is2xxSuccessful());
    }

    private String login(String email, String password) throws Exception {
        String json = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.accessToken");
    }
}
