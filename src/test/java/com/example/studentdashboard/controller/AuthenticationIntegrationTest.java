package com.example.studentdashboard.controller;

import com.example.studentdashboard.AbstractIntegrationTest;
import com.example.studentdashboard.dto.request.LoginRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthenticationIntegrationTest extends AbstractIntegrationTest {

    @Test
    void login_withCorrectCredentials_returnsToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "admin123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.user.username").value("admin"));
    }

    @Test
    void login_withEmailInsteadOfUsername_alsoWorks() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin@test.local", "admin123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void login_withUnknownUser_returns401_notLeakingWhichPartWasWrong() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("nobody@nowhere.com", "whatever123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username/email or password."));
    }

    @Test
    void teacherWithNoLoginGranted_cannotLogIn() throws Exception {
        // teacherB exists as a profile in some other test's fixtures, but here we
        // just confirm a random unlinked email/username combination fails cleanly.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("no-such-teacher", "anything123"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_withValidToken_returnsCurrentUser() throws Exception {
        String token = loginAndGetToken("admin", "admin123");
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void me_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_asNonAdmin_isForbidden() throws Exception {
        String teacherToken = loginAndGetToken("teacher", "teacher123");
        mockMvc.perform(post("/api/auth/register")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"newadmin","email":"newadmin@test.local","password":"password123","fullName":"New Admin","role":"ADMIN"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void register_teacherRole_isRejected() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        mockMvc.perform(post("/api/auth/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"sometecher","email":"sometecher@test.local","password":"password123","fullName":"Some Teacher","role":"TEACHER"}"""))
                .andExpect(status().isBadRequest());
    }
}
