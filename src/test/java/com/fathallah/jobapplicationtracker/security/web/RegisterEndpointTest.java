package com.fathallah.jobapplicationtracker.security.web;


import com.fathallah.jobapplicationtracker.application.repository.JobApplicationRepository;
import com.fathallah.jobapplicationtracker.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.profiles.active=test"})
@AutoConfigureMockMvc
class RegisterEndpointTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    UserRepository userRepository;
    @Autowired
    JobApplicationRepository jobApplicationRepository;

    @BeforeEach
    void setUp() {
        jobApplicationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void validPassword_shouldReturn200() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"user@example.com","password":"ValidPass1!","displayName":"Test User"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "short",             // too short
            "alllowercase1!",    // no uppercase
            "ALLUPPERCASE1!",    // no lowercase
            "NoDigitsHere!",     // no digit
            "NoSpecialChar1",    // no special character
    })
    void weakPassword_shouldReturn400_withValidationError(String password) throws Exception {
        String body = """
            {"email":"user@example.com","password":"%s","displayName":"Test User"}
        """.formatted(password);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.password").isNotEmpty());
    }

    @Test
    void blankPassword_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"email":"user@example.com","password":"","displayName":"Test User"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.password").isNotEmpty());
    }
}
