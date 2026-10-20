package uk.gov.dvla.siftconfig;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ConfigOptionsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getOptions_returns200WithVocabulary() throws Exception {
        mockMvc.perform(get("/api/v1/config-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delimiters").isArray())
                .andExpect(jsonPath("$.formats").isArray())
                .andExpect(jsonPath("$.modes").isArray())
                .andExpect(jsonPath("$.schemaModes").isArray())
                .andExpect(jsonPath("$.maxCorruptPercentDefault").value(0.1));
    }
}
