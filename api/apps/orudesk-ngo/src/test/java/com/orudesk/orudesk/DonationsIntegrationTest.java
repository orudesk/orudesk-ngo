package com.orudesk.orudesk;

import com.jayway.jsonpath.JsonPath;
import com.orudesk.orudesk.shared.EmailMessage;
import com.orudesk.orudesk.shared.RecordingEmailSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Day 56: donations through the whole NGO app, against PostgreSQL. */
@SpringBootTest(properties = {
        "orudesk.identity.first-admin.email=test-admin@orudesk.org",
        "orudesk.identity.first-admin.password=admin-password",
        "orudesk.ngo.name=Asha Kiran Trust",
        "orudesk.ngo.registration-80g=AAATA1234AF20214"
})
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, DonationsIntegrationTest.Mail.class})
class DonationsIntegrationTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class Mail {
        @Bean
        @Primary
        RecordingEmailSender recordingEmailSender() {
            return new RecordingEmailSender();
        }
    }

    @Autowired
    private RecordingEmailSender mail;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void cleanUp() {
        // The receipt emails run after the commit, on another thread: let them finish before the rows go
        await().atMost(Duration.ofSeconds(10)).until(() -> jdbc.queryForObject(
                "SELECT COUNT(*) FROM event_publication WHERE completion_date IS NULL", Integer.class) == 0);
        jdbc.update("DELETE FROM ngo_donation");
        jdbc.update("DELETE FROM ngo_receipt_counter");
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
                INSERT INTO ngo_donation (id, tenant_id, donor_id, amount, received_on, mode,
                                          financial_year, receipt_sequence, receipt_issued_on)
                VALUES (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', ?, 250000, CURRENT_DATE, 'CASH',
                        '2026-27', 999, CURRENT_DATE)
                """, UUID.fromString(donor))).hasMessageContaining("ck_ngo_donation_cash_limit");
    }

    @Test
    void receiptsAreNumberedWithoutGapsPerFinancialYear() throws Exception {
        String token = admin();
        String donor = donor(token);
        LocalDate today = LocalDate.now();
        String thisYear = financialYear(today);
        String lastYear = financialYear(today.minusYears(1));

        String first = receiptNumberOf(token, recordDonation(token, donor, "\"100\"", "CASH", "null", today));
        String older = receiptNumberOf(token, recordDonation(token, donor, "\"100\"", "CASH", "null", today.minusYears(1)));
        String second = receiptNumberOf(token, recordDonation(token, donor, "\"100\"", "CASH", "null", today));

        assertThat(first).isEqualTo(thisYear + "/000001");
        assertThat(second).isEqualTo(thisYear + "/000002");
        assertThat(older).isEqualTo(lastYear + "/000001");   // last year's series, by the date the money came
    }

    @Test
    void twentyReceiptsAtOnceGetTwentyDifferentNumbersAndNoGap() throws Exception {
        String token = admin();
        String donor = donor(token);
        ExecutorService pool = Executors.newFixedThreadPool(10);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                results.add(pool.submit(() -> recordDonation(token, donor, "\"100\"", "CASH", "null")
                        .andReturn().getResponse().getStatus()));
            }
            for (Future<Integer> result : results) {
                assertThat(result.get()).isEqualTo(201);
            }
        } finally {
            pool.shutdown();
        }

        assertThat(jdbc.queryForList("SELECT receipt_sequence FROM ngo_donation ORDER BY receipt_sequence", Integer.class))
                .isEqualTo(IntStream.rangeClosed(1, 20).boxed().toList());
    }

    @Test
    void theReceiptShowsThePanOnlyWhenItIsDeductible() throws Exception {
        String token = admin();
        String donor = donor(token);
        String upi = recordDonation(token, donor, "\"150000.50\"", "UPI", "\"UTR-9\"").andReturn().getResponse()
                .getHeader("Location");
        String cash = recordDonation(token, donor, "\"5000\"", "CASH", "null").andReturn().getResponse()
                .getHeader("Location");

        mockMvc.perform(get(upi + "/receipt").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organisationName").value("Asha Kiran Trust"))
                .andExpect(jsonPath("$.amountInWords").value("Rupees One Lakh Fifty Thousand and Fifty Paise Only"))
                .andExpect(jsonPath("$.taxDeductible").value(true))
                .andExpect(jsonPath("$.donorPan").value("ABCDE1234F"));
        mockMvc.perform(get(cash + "/receipt").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.taxDeductible").value(false))
                .andExpect(jsonPath("$.notDeductibleBecause").value(org.hamcrest.Matchers.containsString("cash above")))
                .andExpect(jsonPath("$.donorPan").doesNotExist());
    }

    @Test
    void aCancelledDonationLeavesTheTotalButKeepsItsReceiptNumber() throws Exception {
        String token = admin();
        String donor = donor(token);
        recordDonation(token, donor, "\"1000\"", "UPI", "\"UTR-1\"").andExpect(status().isCreated());
        String cheque = recordDonation(token, donor, "\"5000\"", "CHEQUE", "\"004512\"").andReturn().getResponse()
                .getHeader("Location");

        cancel(token, cheque, 1, "cheque bounced").andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Edit conflict"));   // version 1 was never read
        cancel(token, cheque, 0, "cheque bounced").andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelReason").value("cheque bounced"))
                .andExpect(jsonPath("$.receiptNumber").value(org.hamcrest.Matchers.endsWith("/000002")));
        cancel(token, cheque, 1, "again").andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Donation already cancelled"));

        mockMvc.perform(get("/api/v1/donors/{id}/donations", donor).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.donations.length()").value(2))
                .andExpect(jsonPath("$.total").value("1000.00"));
        mockMvc.perform(get(cheque + "/receipt").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.cancelReason").value("cheque bounced"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_entry WHERE action = 'donations.donation.cancelled'"
                + " AND summary LIKE '%cheque bounced%'", Integer.class)).isEqualTo(1);
    }

    @Test
    void theDonorIsThankedByEmailWithTheReceipt() throws Exception {
        String token = admin();
        String donor = donor(token);
        recordDonation(token, donor, "\"2500\"", "UPI", "\"UTR-77\"").andExpect(status().isCreated());

        // Every test's donor has this address, and the recorder keeps every email: find this donation's
        await().atMost(Duration.ofSeconds(10)).until(() -> mail.sentTo("asha@example.org").stream()
                .anyMatch(m -> m.text().contains("₹2,500.00")));
        EmailMessage email = mail.sentTo("asha@example.org").stream()
                .filter(m -> m.text().contains("₹2,500.00")).findFirst().orElseThrow();
        assertThat(email.subject()).startsWith("Thank you: receipt ");
        assertThat(email.text()).contains("Asha Kiran Trust", "₹2,500.00", "Rupees Two Thousand Five Hundred Only",
                "section 80G", "ABCDE1234F");
    }

    @Test
    void theYearIsSummedByMonthAndModeWithoutTheCancelledOnes() throws Exception {
        String token = admin();
        String donor = donor(token);
        LocalDate today = LocalDate.now();
        String year = financialYear(today);
        recordDonation(token, donor, "\"1000.25\"", "UPI", "\"U1\"").andExpect(status().isCreated());
        recordDonation(token, donor, "\"500\"", "CASH", "null").andExpect(status().isCreated());
        String bounced = recordDonation(token, donor, "\"9000\"", "CHEQUE", "\"C1\"").andReturn().getResponse()
                .getHeader("Location");
        cancel(token, bounced, 0, "bounced").andExpect(status().isOk());

        String month = today.toString().substring(0, 7);
        mockMvc.perform(get("/api/v1/donations/summary").param("financialYear", year)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value("1500.25"))
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.cancelledCount").value(1))
                .andExpect(jsonPath("$.byMonth.length()").value(12))
                .andExpect(jsonPath("$.byMonth[0].key").value(year.substring(0, 4) + "-04"))
                .andExpect(jsonPath("$.byMonth[?(@.key == '" + month + "')].total").value("1500.25"))
                .andExpect(jsonPath("$.byMode[0].key").value("UPI"));
        mockMvc.perform(get("/api/v1/donations/summary").param("financialYear", "2026-28")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void form10BDListsTheDeductibleDonationsSafelyAsCsv() throws Exception {
        String token = admin();
        String asha = donor(token);
        String formula = donor(token, "=cmd", "Test-Gift", "PQRST6789Z", true);
        String noReceipt = donor(token, "Ravi", "Test-Gift", "LMNOP4321Q", false);
        recordDonation(token, asha, "\"1000\"", "UPI", "\"U1\"").andExpect(status().isCreated());
        recordDonation(token, asha, "\"2500\"", "BANK_TRANSFER", "\"N1\"").andExpect(status().isCreated());
        recordDonation(token, asha, "\"1500\"", "CASH", "null").andExpect(status().isCreated());
        recordDonation(token, asha, "\"5000\"", "CASH", "null").andExpect(status().isCreated());   // cash > 2000: not deductible
        recordDonation(token, formula, "\"700\"", "CARD", "\"T1\"").andExpect(status().isCreated());
        recordDonation(token, noReceipt, "\"900\"", "UPI", "\"U2\"").andExpect(status().isCreated());

        String csv = mockMvc.perform(get("/api/v1/donations/form-10bd").param("financialYear", financialYear(LocalDate.now()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Content-Disposition", org.hamcrest.Matchers.containsString("form-10bd-")))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

        assertThat(csv.split("\r\n")).containsExactly(
                "Sr. No.,ID Code (PAN),Name of donor,Donation type,Mode of receipt,Amount of donation (INR)",
                "1,PQRST6789Z,'=cmd Test-Gift,Others,Electronic modes including account payee cheque/draft,700.00",
                "2,ABCDE1234F,Asha Test-Gift,Others,Electronic modes including account payee cheque/draft,3500.00",
                "3,ABCDE1234F,Asha Test-Gift,Others,Cash,1500.00");
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
        return recordDonation(token, donorId, amount, mode, reference, LocalDate.now());
    }

    private ResultActions recordDonation(String token, String donorId, String amount, String mode, String reference,
                                         LocalDate receivedOn) throws Exception {
        return mockMvc.perform(post("/api/v1/donations").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"donorId": "%s", "amount": %s, "receivedOn": "%s", "mode": "%s", "reference": %s}
                        """.formatted(donorId, amount, receivedOn, mode, reference)));
    }

    private ResultActions cancel(String token, String location, int version, String reason) throws Exception {
        return mockMvc.perform(post(location + "/cancel").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\": %d, \"reason\": \"%s\"}".formatted(version, reason)));
    }

    private String receiptNumberOf(String token, ResultActions created) throws Exception {
        String location = created.andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
        String json = mockMvc.perform(get(location).header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.receiptNumber");
    }

    private static String financialYear(LocalDate date) {
        int start = date.getMonthValue() >= 4 ? date.getYear() : date.getYear() - 1;
        return start + "-" + String.format("%02d", (start + 1) % 100);
    }

    private String donor(String token) throws Exception {
        return donor(token, "Asha", "Test-Gift", "ABCDE1234F", true);
    }

    private String donor(String token, String firstName, String lastName, String pan, boolean wantsTaxReceipt)
            throws Exception {
        String json = mockMvc.perform(post("/api/v1/donors").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "%s", "lastName": "%s", "email": "asha@example.org",
                                 "pan": "%s", "wantsTaxReceipt": %s}
                                """.formatted(firstName, lastName, pan, wantsTaxReceipt)))
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
