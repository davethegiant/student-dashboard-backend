package com.example.studentdashboard.controller;

import com.example.studentdashboard.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AttendanceAndResultIntegrationTest extends AbstractIntegrationTest {

    @Test
    void teacher_canSubmitAttendance_forOwnClass() throws Exception {
        String token = loginAndGetToken("teacher", "teacher123");
        String body = """
                {"classId":%d,"date":"2026-02-20","academicSessionId":%d,"termId":%d,
                 "entries":[{"studentId":%d,"status":"Present"}]}
                """.formatted(classA.getId(), session.getId(), term.getId(), studentInClassA.getId());

        mockMvc.perform(post("/api/attendance")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.present").value(1))
                .andExpect(jsonPath("$.absent").value(0))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.rate").value(100.0));
    }

    @Test
    void teacher_cannotSubmitAttendance_forClassTheyDontTeach() throws Exception {
        String token = loginAndGetToken("teacher", "teacher123");
        String body = """
                {"classId":%d,"date":"2026-02-20","academicSessionId":%d,"termId":%d,
                 "entries":[{"studentId":%d,"status":"Present"}]}
                """.formatted(classB.getId(), session.getId(), term.getId(), studentInClassB.getId());

        mockMvc.perform(post("/api/attendance")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void mixedAttendanceStatuses_computeCorrectRate() throws Exception {
        // Present + Late counts half-weight toward the rate, matching the frontend's formula.
        String token = loginAndGetToken("admin", "admin123");
        String body = """
                {"classId":%d,"date":"2026-02-21","academicSessionId":%d,"termId":%d,
                 "entries":[
                   {"studentId":%d,"status":"Present"},
                   {"studentId":%d,"status":"Late"}
                 ]}
                """.formatted(classA.getId(), session.getId(), term.getId(),
                studentInClassA.getId(), studentInClassA.getId());
        // (Same student appears twice on purpose here purely to get 2 entries without a second fixture student.)

        mockMvc.perform(post("/api/attendance")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.present").value(1))
                .andExpect(jsonPath("$.late").value(1))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.rate").value(75.0)); // (1 + 0.5) / 2 * 100
    }

    @Test
    void admin_canSubmitResult_totalAndGradeAreComputedServerSide() throws Exception {
        String token = loginAndGetToken("admin", "admin123");
        String body = """
                {"studentId":%d,"subjectId":%d,"academicSessionId":%d,"termId":%d,"ca":35,"exam":50,"comment":"Great work"}
                """.formatted(studentInClassA.getId(), mathSubject.getId(), session.getId(), term.getId());

        mockMvc.perform(post("/api/results")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(85))
                .andExpect(jsonPath("$.grade").value("A")); // >=75 -> A
    }

    @Test
    void resubmittingSameStudentSubjectSessionTerm_upsertsRatherThanDuplicates() throws Exception {
        String token = loginAndGetToken("admin", "admin123");
        String firstBody = """
                {"studentId":%d,"subjectId":%d,"academicSessionId":%d,"termId":%d,"ca":10,"exam":20}
                """.formatted(studentInClassA.getId(), mathSubject.getId(), session.getId(), term.getId());
        String secondBody = """
                {"studentId":%d,"subjectId":%d,"academicSessionId":%d,"termId":%d,"ca":35,"exam":55}
                """.formatted(studentInClassA.getId(), mathSubject.getId(), session.getId(), term.getId());

        mockMvc.perform(post("/api/results")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(30));

        mockMvc.perform(post("/api/results")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(90))
                .andExpect(jsonPath("$.grade").value("A"));

        mockMvc.perform(get("/api/results/student/" + studentInClassA.getId())
                        .param("sessionId", session.getId().toString())
                        .param("termId", term.getId().toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)); // one row, not two
    }

    @Test
    void ca_aboveMaximum_isRejectedByValidation() throws Exception {
        String token = loginAndGetToken("admin", "admin123");
        String body = """
                {"studentId":%d,"subjectId":%d,"academicSessionId":%d,"termId":%d,"ca":999,"exam":50}
                """.formatted(studentInClassA.getId(), mathSubject.getId(), session.getId(), term.getId());

        mockMvc.perform(post("/api/results")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void teacher_cannotSubmitResult_forStudentOutsideTheirClass() throws Exception {
        String token = loginAndGetToken("teacher", "teacher123");
        String body = """
                {"studentId":%d,"subjectId":%d,"academicSessionId":%d,"termId":%d,"ca":30,"exam":40}
                """.formatted(studentInClassB.getId(), mathSubject.getId(), session.getId(), term.getId());

        mockMvc.perform(post("/api/results")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }
}
