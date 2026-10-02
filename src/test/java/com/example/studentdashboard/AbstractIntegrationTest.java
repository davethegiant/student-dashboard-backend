package com.example.studentdashboard;

import com.example.studentdashboard.dto.request.LoginRequest;
import com.example.studentdashboard.entity.AcademicSession;
import com.example.studentdashboard.entity.Gender;
import com.example.studentdashboard.entity.RecordStatus;
import com.example.studentdashboard.entity.SchoolClass;
import com.example.studentdashboard.entity.Student;
import com.example.studentdashboard.entity.Subject;
import com.example.studentdashboard.entity.Teacher;
import com.example.studentdashboard.entity.Term;
import com.example.studentdashboard.entity.User;
import com.example.studentdashboard.repository.AcademicSessionRepository;
import com.example.studentdashboard.repository.SchoolClassRepository;
import com.example.studentdashboard.repository.StudentRepository;
import com.example.studentdashboard.repository.SubjectRepository;
import com.example.studentdashboard.repository.TeacherRepository;
import com.example.studentdashboard.repository.TermRepository;
import com.example.studentdashboard.repository.UserRepository;
import com.example.studentdashboard.security.Role;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Shared fixture + MockMvc setup for every integration test.
 * @Transactional here (applied to every subclass) wraps each test method —
 * including its @BeforeEach fixture inserts and every MockMvc call it
 * makes, since MockMvc runs the request in-process on the same thread — in
 * one transaction that's rolled back afterward. Without this, two test
 * classes both inserting a "2025/2026" session/"First Term" term would
 * collide on the unique constraints, since Spring caches and reuses the
 * Spring context (and therefore the H2 database) across test classes with
 * identical configuration.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class AbstractIntegrationTest {

    @Autowired protected WebApplicationContext webApplicationContext;
    @Autowired protected AcademicSessionRepository academicSessionRepository;
    @Autowired protected TermRepository termRepository;
    @Autowired protected SchoolClassRepository schoolClassRepository;
    @Autowired protected SubjectRepository subjectRepository;
    @Autowired protected TeacherRepository teacherRepository;
    @Autowired protected StudentRepository studentRepository;
    @Autowired protected UserRepository userRepository;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected ObjectMapper objectMapper;

    protected MockMvc mockMvc;

    protected AcademicSession session;
    protected Term term;
    protected SchoolClass classA;
    protected SchoolClass classB;
    protected Subject mathSubject;
    /** Assigned to classA only, has login (username "teacher" / password "teacher123"). */
    protected Teacher teacherA;
    protected Student studentInClassA;
    protected Student studentInClassB;

    @BeforeEach
    void baseSetUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        session = academicSessionRepository.save(AcademicSession.builder().label("2025/2026").current(true).build());
        term = termRepository.save(Term.builder().name("First Term").sortOrder(1).current(true).build());
        classA = schoolClassRepository.save(SchoolClass.builder().classCode("c1").name("JSS 1A").level("Junior Secondary").build());
        classB = schoolClassRepository.save(SchoolClass.builder().classCode("c2").name("JSS 1B").level("Junior Secondary").build());
        mathSubject = subjectRepository.save(Subject.builder().subjectCode("MTH").name("Mathematics").build());

        userRepository.save(User.builder()
                .username("admin").email("admin@test.local")
                .password(passwordEncoder.encode("admin123"))
                .fullName("Test Admin").role(Role.ADMIN).build());

        Set<SchoolClass> assignedClasses = new HashSet<>(Set.of(classA));
        Set<Subject> assignedSubjects = new HashSet<>(Set.of(mathSubject));
        teacherA = Teacher.builder()
                .teacherCode("TCH001").name("Mrs. Test Teacher").email("teacher@test.local")
                .status(RecordStatus.Active).joinedDate(LocalDate.of(2020, 1, 1))
                .classes(assignedClasses).subjects(assignedSubjects)
                .build();
        teacherA = teacherRepository.save(teacherA);

        User teacherUser = User.builder()
                .username("teacher").email(teacherA.getEmail())
                .password(passwordEncoder.encode("teacher123"))
                .fullName(teacherA.getName()).role(Role.TEACHER).build();
        teacherA.setUser(teacherUser);
        teacherA = teacherRepository.save(teacherA);

        studentInClassA = studentRepository.save(Student.builder()
                .studentCode("STU0001").name("Alice InClassA").gender(Gender.Female)
                .schoolClass(classA).status(RecordStatus.Active).build());
        studentInClassB = studentRepository.save(Student.builder()
                .studentCode("STU0002").name("Bob InClassB").gender(Gender.Male)
                .schoolClass(classB).status(RecordStatus.Active).build());
    }

    protected String loginAndGetToken(String usernameOrEmail, String password) throws Exception {
        String body = objectMapper.writeValueAsString(new LoginRequest(usernameOrEmail, password));
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(response);
        return node.get("token").asText();
    }
}
