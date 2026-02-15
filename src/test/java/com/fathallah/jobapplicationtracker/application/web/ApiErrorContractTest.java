package com.fathallah.jobapplicationtracker.application.web;

import com.fathallah.jobapplicationtracker.security.JwtService;
import com.fathallah.jobapplicationtracker.security.domain.RoleName;
import com.fathallah.jobapplicationtracker.security.domain.User;
import com.fathallah.jobapplicationtracker.security.repository.RoleRepository;
import com.fathallah.jobapplicationtracker.security.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.profiles.active=test"})
@AutoConfigureMockMvc
class ApiErrorContractTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired JwtService jwtService;

    @BeforeEach
    void setUp() {
        // keep it simple: clear users so tokenFor() can insert safely
        userRepository.deleteAll();
    }

    private String bearerFor(String email) {
        var userRole = roleRepository.findByName(RoleName.ROLE_USER).orElseThrow();

        var user = userRepository.save(User.builder()
                .email(email)
                .displayName(email)
                .passwordHash("x")
                .roles(Set.of(userRole))
                .build());

        var roleNames = user.getRoles().stream().map(r -> r.getName().name()).toList();
        return "Bearer " + jwtService.generateToken(user.getEmail(), roleNames);
    }

    @Test
    void validationError_shouldReturn400_andMatchErrorShape() throws Exception {
        String bearer = bearerFor("a@test.com");

        // missing company, missing dateApplied
        var body = Map.of(
                "position", "Dev",
                "status", "APPLIED"
        );

        mockMvc.perform(post("/api/applications")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))

                // ApiError fields
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value("/api/applications"))

                // validationErrors map content
                .andExpect(jsonPath("$.validationErrors").isMap())
                .andExpect(jsonPath("$.validationErrors.company").exists())
                .andExpect(jsonPath("$.validationErrors.dateApplied").exists());
    }

    @Test
    void notFound_shouldReturn404_andMatchErrorShape() throws Exception {
        String bearer = bearerFor("a@test.com");

        mockMvc.perform(get("/api/applications/{id}", 999999L)
                        .header("Authorization", bearer))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))

                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message", containsString("Application not found")))
                .andExpect(jsonPath("$.path").value("/api/applications/999999"))

                // should be null for non-validation errors
                .andExpect(jsonPath("$.validationErrors").doesNotExist())
                .andExpect(jsonPath("$['validationErrors']").doesNotExist());
    }

    @Test
    void forbidden_shouldReturn403_andMatchErrorShape() throws Exception {
        String bearerA = bearerFor("owner@test.com");
        String bearerB = bearerFor("intruder@test.com");

        // A creates an application
        String createRes = mockMvc.perform(post("/api/applications")
                        .header("Authorization", bearerA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "company", "SecretCorp",
                                "position", "Backend",
                                "status", "APPLIED",
                                "dateApplied", "2026-02-09"
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long id = objectMapper.readTree(createRes).get("id").asLong();

        // B tries to access A's resource => 403
        mockMvc.perform(get("/api/applications/{id}", id)
                        .header("Authorization", bearerB))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))

                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message", anyOf(
                        containsString("Not owner"),
                        containsString("Forbidden")
                )))
                .andExpect(jsonPath("$.path").value("/api/applications/" + id));
    }

    @Test
    void unauthenticated_shouldReturn401_andMatchErrorShape() throws Exception {
        mockMvc.perform(get("/api/applications"))
                .andExpect(status().isUnauthorized());
        // If you configured the Security entry point to return JSON body, we can assert it here too.
        // With default Spring Security, it may not return your ApiError JSON automatically.
    }
}
