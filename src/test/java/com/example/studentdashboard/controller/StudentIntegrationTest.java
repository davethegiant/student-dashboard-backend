package com.example.studentdashboard.controller;

import com.example.studentdashboard.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StudentIntegrationTest extends AbstractIntegrationTest {

    @Test
    void admin_canCreateStudent() throws Exception {
        String token = loginAndGetToken("admin", "admin123");
        String body = """
                {"name":"Charlie NewStudent","gender":"Male","classId":%d,"guardianName":"Guardian Name",
                 "guardianPhone":"08011112222","address":"1 Test St","admittedDate":"2026-01-10"}
                """.formatted(classA.getId());

        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Charlie NewStudent"))
                .andExpect(jsonPath("$.studentCode").exists())
                .andExpect(jsonPath("$.risk.level").value("HIGH")); // no results/attendance yet -> 0% both -> HIGH
    }

    @Test
    void teacher_cannotCreateStudent() throws Exception {
        String token = loginAndGetToken("teacher", "teacher123");
        String body = """
                {"name":"Should Not Save","gender":"Male","classId":%d}
                """.formatted(classA.getId());

        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymous_cannotListStudents() throws Exception {
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void admin_seesAllStudents_teacherSeesOnlyOwnClass() throws Exception {
        String adminToken = loginAndGetToken("admin", "admin123");
        mockMvc.perform(get("/api/students").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2)); // studentInClassA + studentInClassB

        String teacherToken = loginAndGetToken("teacher", "teacher123");
        mockMvc.perform(get("/api/students").header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1)) // only studentInClassA — teacherA teaches classA only
                .andExpect(jsonPath("$.content[0].name").value("Alice InClassA"));
    }

    @Test
    void teacher_cannotViewProfileOfStudentOutsideTheirClasses() throws Exception {
        String teacherToken = loginAndGetToken("teacher", "teacher123");
        mockMvc.perform(get("/api/students/" + studentInClassB.getId())
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void teacher_canViewProfileOfStudentInOwnClass() throws Exception {
        String teacherToken = loginAndGetToken("teacher", "teacher123");
        mockMvc.perform(get("/api/students/" + studentInClassA.getId())
                        .header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice InClassA"));
    }

    @Test
    void admin_canDeactivateThenReactivateStudent() throws Exception {
        String token = loginAndGetToken("admin", "admin123");

        mockMvc.perform(patch("/api/students/" + studentInClassA.getId() + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"Inactive\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/students/" + studentInClassA.getId() + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"Active\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void admin_canUpdateThenDeleteStudent() throws Exception {
        String token = loginAndGetToken("admin", "admin123");
        String updateBody = """
                {"name":"Alice Updated Name","gender":"Female","classId":%d}
                """.formatted(classA.getId());

        mockMvc.perform(put("/api/students/" + studentInClassA.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice Updated Name"));

        mockMvc.perform(delete("/api/students/" + studentInClassA.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/students/" + studentInClassA.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void creatingStudent_withNonExistentClass_returns404() throws Exception {
        String token = loginAndGetToken("admin", "admin123");
        String body = """
                {"name":"Orphan Student","gender":"Male","classId":999999}
                """;
        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }
}
