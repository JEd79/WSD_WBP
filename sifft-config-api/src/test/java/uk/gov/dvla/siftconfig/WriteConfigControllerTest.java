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

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.gov.dvla.siftconfig.repository.WriteConfigRepository;

@SpringBootTest
@AutoConfigureMockMvc
class WriteConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WriteConfigRepository writeConfigRepository;

    private static final String VALID_APPEND = """
            {
              "targetTable": "drivers.curated.customers",
              "format": "delta",
              "mode": "append",
              "schemaMode": "strict",
              "addSourceMetadata": true
            }
            """;

    @BeforeEach
    void clean() {
        writeConfigRepository.deleteAll();
    }

    private String createValidAndReturnId() throws Exception {
        String response = mockMvc.perform(post("/api/v1/write-configs")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_APPEND))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    // ---------- W3 POST create ----------

    @Test
    void createValidAppend_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/write-configs")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_APPEND))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.format").value("delta"))
                .andExpect(jsonPath("$.mode").value("append"));
    }

    @Test
    void createValidMerge_returns201() throws Exception {
        String body = """
                { "targetTable": "t.s.c", "format": "delta", "mode": "merge",
                  "schemaMode": "merge", "mergeKeys": ["id"] }
                """;
        mockMvc.perform(post("/api/v1/write-configs")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mergeKeys[0]").value("id"));
    }

    @Test
    void createMissingRequiredField_returns400() throws Exception {
        String body = """
                { "format": "delta", "mode": "append", "schemaMode": "strict" }
                """; // targetTable missing
        mockMvc.perform(post("/api/v1/write-configs")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorType").value("validation_error"));
    }

    @Test
    void createInvalidTableIdentifier_returns400() throws Exception {
        String body = """
                { "targetTable": "bad;name", "format": "delta", "mode": "append",
                  "schemaMode": "strict" }
                """;
        mockMvc.perform(post("/api/v1/write-configs")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorType").value("invalid_input"));
    }

    @Test
    void createInvalidFormat_returns422() throws Exception {
        String body = """
                { "targetTable": "t", "format": "avro", "mode": "append",
                  "schemaMode": "strict" }
                """;
        mockMvc.perform(post("/api/v1/write-configs")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorType").value("invalid_format"));
    }

    @Test
    void createInvalidMode_returns422() throws Exception {
        String body = """
                { "targetTable": "t", "format": "delta", "mode": "upsert",
                  "schemaMode": "strict" }
                """;
        mockMvc.perform(post("/api/v1/write-configs")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorType").value("invalid_mode"));
    }

    @Test
    void createInvalidSchemaMode_returns422() throws Exception {
        String body = """
                { "targetTable": "t", "format": "delta", "mode": "append",
                  "schemaMode": "loose" }
                """;
        mockMvc.perform(post("/api/v1/write-configs")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorType").value("invalid_schema_mode"));
    }

    @Test
    void createMergeWithoutKeys_returns422() throws Exception {
        String body = """
                { "targetTable": "t", "format": "delta", "mode": "merge",
                  "schemaMode": "merge" }
                """;
        mockMvc.perform(post("/api/v1/write-configs")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorType").value("merge_key_missing"));
    }

    @Test
    void createMergeOnNonDelta_returns422() throws Exception {
        String body = """
                { "targetTable": "t", "format": "parquet", "mode": "merge",
                  "schemaMode": "merge", "mergeKeys": ["id"] }
                """;
        mockMvc.perform(post("/api/v1/write-configs")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorType").value("invalid_mode"));
    }

    // ---------- W2 GET one ----------

    @Test
    void getMissing_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/write-configs/{id}", "nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorType").value("write_config_not_found"));
    }

    // ---------- W4 PUT ----------

    @Test
    void replaceExisting_returns200() throws Exception {
        String id = createValidAndReturnId();
        String updated = """
                { "targetTable": "drivers.curated.customers", "format": "parquet",
                  "mode": "overwrite", "schemaMode": "overwrite" }
                """;
        mockMvc.perform(put("/api/v1/write-configs/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON).content(updated))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.format").value("parquet"))
                .andExpect(jsonPath("$.mode").value("overwrite"));
    }

    @Test
    void replaceMissing_returns404() throws Exception {
        mockMvc.perform(put("/api/v1/write-configs/{id}", "nope")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_APPEND))
                .andExpect(status().isNotFound());
    }

    // ---------- W5 DELETE ----------

    @Test
    void deleteExisting_returns204() throws Exception {
        String id = createValidAndReturnId();
        mockMvc.perform(delete("/api/v1/write-configs/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteMissing_returns404() throws Exception {
        mockMvc.perform(delete("/api/v1/write-configs/{id}", "nope"))
                .andExpect(status().isNotFound());
    }
}
