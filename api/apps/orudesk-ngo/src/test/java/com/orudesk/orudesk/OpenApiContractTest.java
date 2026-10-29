package com.orudesk.orudesk;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The NGO edition's contract: the core's endpoints plus the donors'. The web app's typed client is generated
 * from it, so this test saves it to build/openapi/orudesk-ngo.json, like the core's does.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OpenApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void theContractHasTheCoresAndTheDonorsOperationsAndIsSaved() throws Exception {
        String json = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<String> operationIds = JsonPath.read(json, "$.paths.*.*.operationId");
        assertThat(operationIds)
                .doesNotHaveDuplicates()
                .allMatch(id -> id.matches("[a-z][a-zA-Z0-9]+"), "camelCase (digits allowed: exportForm10BD), no springdoc suffix like register_1")
                .contains("login", "searchPeople", "registerDonor", "searchDonors", "countDonors", "getDonor");

        // The JSpecify nullability converter comes from the core, and works for the vertical's records too
        List<String> required = JsonPath.read(json, "$.components.schemas.DonorListItem.required");
        assertThat(required).contains("id", "firstName", "type").doesNotContain("phone");

        Path file = Path.of("build/openapi/orudesk-ngo.json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, json);
    }
}
