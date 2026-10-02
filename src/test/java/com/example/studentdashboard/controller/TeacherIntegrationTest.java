package com.example.studentdashboard.controller;

import com.example.studentdashboard.AbstractIntegrationTest;
import com.example.studentdashboard.dto.request.LoginRequest;
import com.example.studentdashboard.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers the Admin -> Add Teacher (+ grant login in the same call) flow:
 * both the Teacher and User rows are created transactionally, the teacher
 * can immediately authenticate with TEACHER (never ADMIN) permissions, and
 * bad input is rejected cleanly instead of leaking a raw DB error.
 */
class TeacherIntegrationTest extends AbstractIntegrationTest {

    private String createTeacherPayload(String name, String email, boolean grantLogin, String username, String password) {
        return """
                {
                  "profile": { "name": "%s", "email": "%s" },
                  "grantLogin": %s,
                  "loginUsername": %s,
                  "loginPassword": %s
                }
                """.formatted(
                name, email, grantLogin,
                username == null ? "null" : "\"" + username + "\"",
                password == null ? "null" : "\"" + password + "\"");
    }

    @Test
    void adminCreatesTeacherWithLogin_createsBothRecords_hashesPassword_andAssignsTeacherRole() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");

        mockMvc.perform(post("/api/teachers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createTeacherPayload("John Doe", "john@school.com", true, "john.doe", "John@12345")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.hasLogin").value(true));

        Optional<User> created = userRepository.findByUsernameIgnoreCase("john.doe");
        assertThat(created).isPresent();
        assertThat(created.get().getRole().name()).isEqualTo("TEACHER");
        assertThat(created.get().getPassword()).isNotEqualTo("John@12345");
        assertThat(passwordEncoder.matches("John@12345", created.get().getPassword())).isTrue();
        assertThat(teacherRepository.findAll())
                .filteredOn(t -> "John Doe".equals(t.getName()))
                .first()
                .satisfies(t -> assertThat(t.getUser()).isNotNull());
    }

    @Test
    void newlyCreatedTeacher_canLogInImmediately_withTeacherPermissionsOnly() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        mockMvc.perform(post("/api/teachers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createTeacherPayload("Jane Roe", "jane@school.com", true, "jane.roe", "Jane@12345")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("jane.roe", "Jane@12345"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.user.role").value("TEACHER"));

        // The same teacher creation flow must never grant ADMIN.
        mockMvc.perform(post("/api/teachers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"profile": {"name": "X", "email": "x@school.com"}, "grantLogin": false}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hasLogin").value(false));
    }

    @Test
    void createTeacher_withoutAdminRole_isForbidden() throws Exception {
        String teacherToken = loginAndGetToken("teacher", "teacher123");
        mockMvc.perform(post("/api/teachers")
                        .header("Authorization", "Bearer " + teacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createTeacherPayload("Blocked", "blocked@school.com", false, null, null)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createTeacher_withDuplicateUsername_isRejected() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");

        // "teacher" already exists from the base fixture's demo teacher account.
        // (Not asserting DB state here: this test runs inside the same ambient
        // transaction as the failing call — see AbstractIntegrationTest's class
        // javadoc — so a same-transaction read would still see the row pending
        // rollback even though createTeacher's own @Transactional boundary, the
        // outermost one in the real running app, rolls it back immediately.)
        mockMvc.perform(post("/api/teachers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createTeacherPayload("Dupe", "dupe@school.com", true, "teacher", "Dupe@12345")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("already taken")));
    }

    @Test
    void createTeacher_grantLoginTrue_missingUsername_isRejected() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        mockMvc.perform(post("/api/teachers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createTeacherPayload("No Username", "nouser@school.com", true, null, "Password@123")))
                .andExpect(status().isBadRequest());
        assertThat(teacherRepository.findAll()).noneMatch(t -> "No Username".equals(t.getName()));
    }

    @Test
    void createTeacher_grantLoginTrue_missingPassword_isRejected() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        mockMvc.perform(post("/api/teachers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createTeacherPayload("No Password", "nopass@school.com", true, "no.password", null)))
                .andExpect(status().isBadRequest());
        assertThat(teacherRepository.findAll()).noneMatch(t -> "No Password".equals(t.getName()));
    }

    @Test
    void createTeacher_withoutGrantLogin_createsProfileOnlyWithNoOrphanUser() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        mockMvc.perform(post("/api/teachers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"profile": {"name": "No Login Yet", "email": "nologin@school.com"}, "grantLogin": false}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hasLogin").value(false));

        assertThat(userRepository.findByEmailIgnoreCase("nologin@school.com")).isEmpty();
    }
}
