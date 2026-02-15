package com.fathallah.jobapplicationtracker.application.web;

import com.fathallah.jobapplicationtracker.application.domain.ApplicationStatus;
import com.fathallah.jobapplicationtracker.application.repository.JobApplicationRepository;
import com.fathallah.jobapplicationtracker.security.JwtService;
import com.fathallah.jobapplicationtracker.security.domain.*;
import com.fathallah.jobapplicationtracker.security.repository.*;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.profiles.active=test"})
@AutoConfigureMockMvc
class JobApplicationControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired JobApplicationRepository repository;

    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired JwtService jwtService;
    @Autowired ObjectMapper objectMapper;
    String token;

    @BeforeEach
    void setUp() {

        repository.deleteAll();
        userRepository.deleteAll();

        var userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow();

        var user = User.builder()
                .email("test@example.com")
                .displayName("Test User")
                .passwordHash("{noop}password") //not used because filters disabled
                .roles(Set.of(userRole))
                .build();

        userRepository.save(user);

        var roleNames = user.getRoles().stream().map(r -> r.getName().name()).toList();
        token = jwtService.generateToken(user.getEmail(), roleNames);
    }

    private String bearer(){
        return "Bearer " + token;
    }

    private String tokenFor(String email) {
        var userRole = roleRepository.findByName(RoleName.ROLE_USER).orElseThrow();
        var user = userRepository.save(User.builder()
                .email(email)
                .displayName(email)
                .passwordHash("x")
                .roles(Set.of(userRole))
                .build());

        var roleNames = user.getRoles().stream().map(r -> r.getName().name()).toList();
        return jwtService.generateToken(user.getEmail(), roleNames);
    }


    @Test
    void create_shouldReturn201_andPersist() throws Exception {
        var body = Map.of(
                "company", "Google",
                "position", "Software Engineer",
                "status", "APPLIED",
                "dateApplied", "2026-02-09",
                "notes", "Applied via careers"
        );

        mockMvc.perform(post("/api/applications")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.company").value("Google"))
                .andExpect(jsonPath("$.status").value("APPLIED"));

        // extra check: really in DB
        org.junit.jupiter.api.Assertions.assertEquals(1, repository.count());

        }

    @Test
    void create_invalid_shouldReturn400() throws Exception {
        // Missing company (required)
        var body = Map.of(
                "position", "Software Engineer",
                "status", "APPLIED",
                "dateApplied", "2026-02-09"
        );

        mockMvc.perform(post("/api/applications")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAll_shouldReturn200_andList() throws Exception {
        // Create two records first
        mockMvc.perform(post("/api/applications")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "company", "A",
                                "position", "Dev",
                                "status", "APPLIED",
                                "dateApplied", "2026-02-09"
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/applications")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "company", "B",
                                "position", "QA",
                                "status", "INTERVIEW",
                                "dateApplied", "2026-02-08"
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/applications")
                        .header("Authorization", bearer())
                        .param("page", "0")
                        .param("size", "1")
                        .param("sort", "company,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getById_shouldReturn200() throws Exception {
        String response = mockMvc.perform(post("/api/applications")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "company", "Netflix",
                                "position", "Backend",
                                "status", "APPLIED",
                                "dateApplied", "2026-02-09"
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/applications/{id}", id)
                        .header("Authorization", bearer())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.company").value("Netflix"));
    }

    @Test
    void patchStatus_shouldReturn200_andUpdate() throws Exception {
        String response = mockMvc.perform(post("/api/applications")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "company", "Amazon",
                                "position", "SWE",
                                "status", "APPLIED",
                                "dateApplied", "2026-02-09"
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(patch("/api/applications/{id}/status", id)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "INTERVIEW"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("INTERVIEW"));
    }

    @Test
    void patchStatus_nonExistingId_shouldReturn404() throws Exception {
        mockMvc.perform(patch("/api/applications/{id}/status", 999999L)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", ApplicationStatus.INTERVIEW.name()
                        ))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Application not found")));
    }

    @Test
    void put_shouldReturn200_andUpdatedObject() throws Exception {
        String createJson = """
                {"company":"A", "position":"Dev","status":"APPLIED","dateApplied":"2026-02-09","notes":null}
                """;
        String response = mockMvc.perform(post("/api/applications")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Integer id = JsonPath.read(response, "$.id");
        String putJson = """
                {"company":"B", "position":"QA","status":"INTERVIEW","dateApplied":"2026-02-10","notes":"updated"}
                """;
        mockMvc.perform(put("/api/applications/{id}", id)
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(putJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.company").value("B"))
                .andExpect(jsonPath("$.position").value("QA"))
                .andExpect(jsonPath("$.status").value("INTERVIEW"))
                .andExpect(jsonPath("$.dateApplied").value("2026-02-10"))
                .andExpect(jsonPath("$.notes").value("updated"));
    }
    @Test
    void unauthorized_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/applications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCannotAccessAnotherUsersApplication_shouldReturn403() throws Exception {

        // ---------- USER A ----------
        String tokenA = tokenFor("userA@example.com");

        String response = mockMvc.perform(post("/api/applications")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "company", "SecretCorp",
                                "position", "Backend",
                                "status", "APPLIED",
                                "dateApplied", "2026-02-09"
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long appId = objectMapper.readTree(response).get("id").asLong();


        // ---------- USER B ----------
        String tokenB = tokenFor("userB@example.com");

        // B tries to GET A's application → 403
        mockMvc.perform(get("/api/applications/{id}", appId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // B tries to DELETE A's application → 403
        mockMvc.perform(delete("/api/applications/{id}", appId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());

        // B tries to UPDATE A's application → 403
        mockMvc.perform(patch("/api/applications/{id}/status", appId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "INTERVIEW"
                        ))))
                .andExpect(status().isForbidden());
    }

}
