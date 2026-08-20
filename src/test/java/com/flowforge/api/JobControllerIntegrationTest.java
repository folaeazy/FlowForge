package com.flowforge.api;

import com.flowforge.BaseRedisIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;


import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class JobControllerIntegrationTest extends BaseRedisIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAcceptValidJobSubmission() throws Exception {
        String requestBody = """
            {
              "tenantId": "tenant-api-test",
              "type": "TEST_JOB",
              "payload": {"key": "value"}
            }
            """;

        mockMvc.perform(post("/api/jobs")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").exists());
    }

    @Test
    void shouldRejectInvalidJobMissingTenantId() throws Exception {
        String requestBody = """
            {
              "type": "TEST_JOB",
              "payload": {}
            }
            """;

        mockMvc.perform(post("/api/jobs")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }
}
