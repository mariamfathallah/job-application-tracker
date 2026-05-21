package com.fathallah.jobapplicationtracker.application.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest(properties = {"spring.profiles.active=test"})
@AutoConfigureMockMvc
class OpenApiDocTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void apiDocs_loads_andContainsExpectedTags() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Job Application Tracker API"))
                .andExpect(jsonPath("$.tags[?(@.name == 'Auth')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name == 'Applications')]").exists());
    }

    @Test
    void apiDocs_registerEndpoint_exists_andHasNoOperationLevelSecurity() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/auth/register'].post").exists())
                .andExpect(jsonPath("$.paths['/api/auth/register'].post.security").doesNotExist());
    }

    @Test
    void apiDocs_applicationsEndpoint_exists() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/applications'].get").exists())
                .andExpect(jsonPath("$.paths['/api/applications'].post").exists());
    }
}
