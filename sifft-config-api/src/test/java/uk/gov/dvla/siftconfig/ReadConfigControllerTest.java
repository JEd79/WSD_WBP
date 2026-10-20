package uk.gov.dvla.siftconfig;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.gov.dvla.siftconfig.repository.ReadConfigRepository;
import uk.gov.dvla.siftconfig.repository.WriteConfigRepository;

@SpringBootTest
@AutoConfigureMockMvc
class ReadConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReadConfigRepository readConfigRepository;

    @Autowired
    private WriteConfigRepository writeConfigRepository;

    private static final String VALID_BODY = """
            {
              "pattern": "Customers\\\\.csv",
              "delimiter": ",",
              "header": "auto",
              "columns": ["customer_id", "name", "age", "status"],
              "maxCorruptPercent": 0.1
            }
            """;

    @BeforeEach
    void clean() {
        readConfigRepository.deleteAll();
        writeConfigRepository.deleteAll();
    }

    private String createValidAndReturnId() throws Exception {
        String response = mockMvc.perform(post("/api/v1/read-configs")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(response);
        return node.get("id").asText();
    }

    // ---------- R3 POST create ----------

    @Test
    void createValid_returns201WithLocationAndBody() throws Exception {
        mockMvc.perform(post("/api/v1/read-configs")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.pattern").value("Customers\\.csv"))
                .andExpect(jsonPath("$.delimiter").value(","))
                .andExpect(jsonPath("$.maxCorruptPercent").value(0.1));
    }

    @Test
    void createMissingRequiredField_returns400() throws Exception {
        String body = """
                { "delimiter": ",", "header": "auto", "columns": ["a"] }
                """; // pattern missing
        mockMvc.perform(post("/api/v1/read-configs")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorType").value("validation_error"));
    }

    @Test
    void createMultiCharDelimiter_returns400() throws Exception {
        String body = """
                { "pattern": "X\\\\.csv", "delimiter": ";;", "header": "auto", "columns": ["a"] }
                """;
        mockMvc.perform(post("/api/v1/read-configs")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorType").value("invalid_delimiter"));
    }

    @Test
    void createInvalidHeader_returns400() throws Exception {
        String body = """
                { "pattern": "X\\\\.csv", "delimiter": ",", "header": "maybe", "columns": ["a"] }
                """;
        mockMvc.perform(post("/api/v1/read-configs")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorType").value("invalid_header"));
    }

    @Test
    void createEmptyColumns_returns400() throws Exception {
        String body = """
                { "pattern": "X\\\\.csv", "delimiter": ",", "header": "auto", "columns": [] }
                """;
        mockMvc.perform(post("/api/v1/read-configs")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorType").value("validation_error"));
    }

    @Test
    void createBadRegexPattern_returns400() throws Exception {
        String body = """
                { "pattern": "[unclosed", "delimiter": ",", "header": "auto", "columns": ["a"] }
                """;
        mockMvc.perform(post("/api/v1/read-configs")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorType").value("invalid_pattern"));
    }

    @Test
    void createDuplicatePattern_returns409() throws Exception {
        createValidAndReturnId();
        mockMvc.perform(post("/api/v1/read-configs")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorType").value("duplicate_pattern"));
    }

    @Test
    void createWithUnknownWriteConfigId_returns422() throws Exception {
        String body = """
                { "pattern": "Y\\\\.csv", "delimiter": ",", "header": "auto",
                  "columns": ["a"], "writeConfigId": "does-not-exist" }
                """;
        mockMvc.perform(post("/api/v1/read-configs")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorType").value("write_config_not_found"));
    }

    @Test
    void createMalformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/read-configs")
                        .contentType(MediaType.APPLICATION_JSON).content("{ not json "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorType").value("malformed_request"));
    }

    // ---------- R2 GET one ----------

    @Test
    void getExisting_returns200() throws Exception {
        String id = createValidAndReturnId();
        mockMvc.perform(get("/api/v1/read-configs/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void getMissing_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/read-configs/{id}", "nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorType").value("read_config_not_found"));
    }

    // ---------- R4 PUT replace ----------

    @Test
    void replaceExisting_returns200() throws Exception {
        String id = createValidAndReturnId();
        String updated = """
                { "pattern": "Customers\\\\.csv", "delimiter": "|", "header": "true",
                  "columns": ["customer_id", "name"] }
                """;
        mockMvc.perform(put("/api/v1/read-configs/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON).content(updated))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delimiter").value("|"))
                .andExpect(jsonPath("$.header").value("true"));
    }

    @Test
    void replaceMissing_returns404() throws Exception {
        mockMvc.perform(put("/api/v1/read-configs/{id}", "nope")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound());
    }

    @Test
    void replaceWithInvalidDelimiter_returns400() throws Exception {
        String id = createValidAndReturnId();
        String bad = """
                { "pattern": "Customers\\\\.csv", "delimiter": "||", "header": "true",
                  "columns": ["a"] }
                """;
        mockMvc.perform(put("/api/v1/read-configs/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON).content(bad))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorType").value("invalid_delimiter"));
    }

    // ---------- R5 DELETE ----------

    @Test
    void deleteExisting_returns204() throws Exception {
        String id = createValidAndReturnId();
        mockMvc.perform(delete("/api/v1/read-configs/{id}", id))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/read-configs/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteMissing_returns404() throws Exception {
        mockMvc.perform(delete("/api/v1/read-configs/{id}", "nope"))
                .andExpect(status().isNotFound());
    }

    // ---------- R6 POST test (server-side processing) ----------

    @Test
    void testConfigMatchingLine_returns200Matches() throws Exception {
        String id = createValidAndReturnId();
        mockMvc.perform(post("/api/v1/read-configs/{id}/test", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sampleLine\": \"1,Alice,34,active\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detectedFieldCount").value(4))
                .andExpect(jsonPath("$.expectedFieldCount").value(4))
                .andExpect(jsonPath("$.matches").value(true));
    }

    @Test
    void testConfigMismatchedLine_returns200NoMatch() throws Exception {
        String id = createValidAndReturnId();
        mockMvc.perform(post("/api/v1/read-configs/{id}/test", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sampleLine\": \"1,Alice\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detectedFieldCount").value(2))
                .andExpect(jsonPath("$.matches").value(false));
    }

    @Test
    void testConfigMissingId_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/read-configs/{id}/test", "nope")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sampleLine\": \"a,b\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testConfigMissingSampleLine_returns400() throws Exception {
        String id = createValidAndReturnId();
        mockMvc.perform(post("/api/v1/read-configs/{id}/test", id)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorType").value("validation_error"));
    }
}
