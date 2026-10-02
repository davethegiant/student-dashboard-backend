package com.example.studentdashboard.seed;

import com.example.studentdashboard.config.SeedProperties;
import com.example.studentdashboard.entity.AcademicSession;
import com.example.studentdashboard.entity.ActivityLogEntry;
import com.example.studentdashboard.entity.Attendance;
import com.example.studentdashboard.entity.AttendanceStatus;
import com.example.studentdashboard.entity.Gender;
import com.example.studentdashboard.entity.Notification;
import com.example.studentdashboard.entity.RecordStatus;
import com.example.studentdashboard.entity.Result;
import com.example.studentdashboard.entity.SchoolClass;
import com.example.studentdashboard.entity.Student;
import com.example.studentdashboard.entity.Subject;
import com.example.studentdashboard.entity.Teacher;
import com.example.studentdashboard.entity.Term;
import com.example.studentdashboard.entity.User;
import com.example.studentdashboard.repository.AcademicSessionRepository;
import com.example.studentdashboard.repository.ActivityLogEntryRepository;
import com.example.studentdashboard.repository.AttendanceRepository;
import com.example.studentdashboard.repository.NotificationRepository;
import com.example.studentdashboard.repository.ResultRepository;
import com.example.studentdashboard.repository.SchoolClassRepository;
import com.example.studentdashboard.repository.StudentRepository;
import com.example.studentdashboard.repository.SubjectRepository;
import com.example.studentdashboard.repository.TeacherRepository;
import com.example.studentdashboard.repository.TermRepository;
import com.example.studentdashboard.repository.UserRepository;
import com.example.studentdashboard.security.Role;
import com.example.studentdashboard.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * The real seeding logic, in its own Spring bean so @Transactional actually
 * applies (a CommandLineRunner calling a @Transactional method on itself
 * would bypass the proxy — see DataSeeder, which just delegates here).
 * Deterministic seed (fixed Random) so re-running against a wiped dev DB
 * produces the same demo dataset each time.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DemoDataSeedService {

    private final AcademicSessionRepository academicSessionRepository;
    private final TermRepository termRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final ResultRepository resultRepository;
    private final AttendanceRepository attendanceRepository;
    private final NotificationRepository notificationRepository;
    private final ActivityLogEntryRepository activityLogEntryRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SeedProperties seedProperties;

    private final Random random = new Random(20260911L);

    private static final String[] MALE_FIRST = {"Chidi", "Emeka", "Tunde", "Segun", "Ibrahim", "Musa", "David",
            "Samuel", "Daniel", "Kelechi", "Obinna", "Yusuf", "Femi", "Bayo", "Chike", "Uche", "Wale", "Ahmed",
            "Nnamdi", "Kayode", "Peter", "Victor", "Michael", "Joseph", "Isaac"};
    private static final String[] FEMALE_FIRST = {"Ngozi", "Amara", "Funke", "Bisi", "Chiamaka", "Aisha", "Grace",
            "Blessing", "Zainab", "Temitope", "Adaeze", "Fatima", "Chioma", "Yetunde", "Halima", "Faith", "Comfort",
            "Rita", "Kemi", "Ify", "Amina", "Esther", "Patience", "Precious", "Success"};
    private static final String[] LAST_NAMES = {"Okafor", "Adeyemi", "Balogun", "Eze", "Abubakar", "Nwosu",
            "Okonkwo", "Yusuf", "Adebayo", "Ibrahim", "Chukwu", "Ogunleye", "Suleiman", "Danjuma", "Okoro",
            "Afolabi", "Uzo", "Mustapha", "Onyekwere", "Bello", "Ajayi", "Njoku", "Lawal", "Emeka", "Oladipo"};
    private static final String[] STREETS = {"Adeola Odeku St", "Allen Ave", "Awolowo Rd", "Herbert Macaulay Way",
            "Ikorodu Rd", "Opebi Rd", "Ligali Ayorinde St", "Bode Thomas St"};

    @Transactional
    public void seedIfEmpty() {
        if (academicSessionRepository.count() > 0) {
            log.info("Database already contains data — skipping demo seed.");
            return;
        }

        log.info("Seeding demo data (this runs once)...");

        List<AcademicSession> sessions = seedSessions();
        List<Term> terms = seedTerms();
        List<SchoolClass> classes = seedClasses();
        List<Subject> subjects = seedSubjects();
        List<Teacher> teachers = seedTeachers(classes, subjects);
        assignFormTeachers(classes, teachers);
        List<Student> students = seedStudents(classes);
        seedResultsAndAttendance(students, sessions, terms, subjects);
        seedNotificationsAndActivity();
        User admin = seedAdmin();
        Teacher demoTeacher = grantDemoTeacherLogin(teachers);

        log.info("Demo data seeding complete: {} classes, {} subjects, {} teachers, {} students.",
                classes.size(), subjects.size(), teachers.size(), students.size());
        printCredentials(admin, demoTeacher);
    }

    // -------------------------------------------------------------------
    // Reference data
    // -------------------------------------------------------------------

    private List<AcademicSession> seedSessions() {
        AcademicSession s1 = academicSessionRepository.save(
                AcademicSession.builder().label("2025/2026").current(true).build());
        AcademicSession s2 = academicSessionRepository.save(
                AcademicSession.builder().label("2026/2027").current(false).build());
        return List.of(s1, s2);
    }

    private List<Term> seedTerms() {
        Term t1 = termRepository.save(Term.builder().name("First Term").sortOrder(1).current(false).build());
        Term t2 = termRepository.save(Term.builder().name("Second Term").sortOrder(2).current(true).build());
        Term t3 = termRepository.save(Term.builder().name("Third Term").sortOrder(3).current(false).build());
        return List.of(t1, t2, t3);
    }

    private List<SchoolClass> seedClasses() {
        String[][] defs = {
                {"c1", "JSS 1A", "Junior Secondary"}, {"c2", "JSS 1B", "Junior Secondary"},
                {"c3", "JSS 2A", "Junior Secondary"}, {"c4", "JSS 2B", "Junior Secondary"},
                {"c5", "JSS 3A", "Junior Secondary"}, {"c6", "SS 1A", "Senior Secondary"},
                {"c7", "SS 1B", "Senior Secondary"}, {"c8", "SS 2A", "Senior Secondary"},
                {"c9", "SS 2B", "Senior Secondary"}, {"c10", "SS 3A", "Senior Secondary"},
        };
        List<SchoolClass> classes = new ArrayList<>();
        for (String[] d : defs) {
            classes.add(schoolClassRepository.save(SchoolClass.builder()
                    .classCode(d[0]).name(d[1]).level(d[2]).build()));
        }
        return classes;
    }

    private List<Subject> seedSubjects() {
        String[][] defs = {
                {"MTH", "Mathematics"}, {"ENG", "English Language"}, {"BSC", "Basic Science"},
                {"SST", "Social Studies"}, {"CIV", "Civic Education"}, {"CMP", "Computer Studies"},
                {"AGR", "Agricultural Science"}, {"FRN", "French"},
        };
        List<Subject> subjects = new ArrayList<>();
        for (String[] d : defs) {
            subjects.add(subjectRepository.save(Subject.builder().subjectCode(d[0]).name(d[1]).build()));
        }
        return subjects;
    }

    // -------------------------------------------------------------------
    // Name generation helpers
    // -------------------------------------------------------------------

    private <T> T pick(T[] pool) {
        return pool[random.nextInt(pool.length)];
    }

    private int randInt(int minInclusive, int maxInclusive) {
        return minInclusive + random.nextInt(maxInclusive - minInclusive + 1);
    }

    private double randDouble(double min, double max) {
        return min + random.nextDouble() * (max - min);
    }

    private String fullName(Gender gender) {
        String first = gender == Gender.Male ? pick(MALE_FIRST) : pick(FEMALE_FIRST);
        return first + " " + pick(LAST_NAMES);
    }

    // -------------------------------------------------------------------
    // Staff
    // -------------------------------------------------------------------

    private List<Teacher> seedTeachers(List<SchoolClass> classes, List<Subject> subjects) {
        List<Teacher> teachers = new ArrayList<>();
        Set<String> usedEmails = new HashSet<>();

        for (int i = 0; i < 16; i++) {
            Gender gender = random.nextDouble() > 0.45 ? Gender.Female : Gender.Male;
            String title = gender == Gender.Male ? "Mr." : "Mrs.";

            String name;
            String email;
            do {
                String firstLast = fullName(gender);
                name = title + " " + firstLast;
                email = (title + "." + firstLast).toLowerCase()
                        .replaceAll("[^a-z.]+", ".").replaceAll("\\.+", ".") + "@brightfield.edu.ng";
            } while (!usedEmails.add(email));

            Set<Subject> teacherSubjects = new HashSet<>();
            int subjectCount = randInt(1, 2);
            while (teacherSubjects.size() < subjectCount) {
                teacherSubjects.add(pick(subjects.toArray(new Subject[0])));
            }
            Set<SchoolClass> teacherClasses = new HashSet<>();
            int classCount = randInt(1, 2);
            while (teacherClasses.size() < classCount) {
                teacherClasses.add(pick(classes.toArray(new SchoolClass[0])));
            }

            Teacher teacher = Teacher.builder()
                    .teacherCode(CodeGenerator.tempCode())
                    .name(name)
                    .email(email)
                    .phone("080" + randInt(10000000, 99999999))
                    .status(random.nextDouble() > 0.06 ? RecordStatus.Active : RecordStatus.Inactive)
                    .joinedDate(LocalDate.of(randInt(2015, 2024), randInt(1, 9), randInt(1, 28)))
                    .classes(teacherClasses)
                    .subjects(teacherSubjects)
                    .build();
            teacher = teacherRepository.save(teacher);
            teacher.setTeacherCode(CodeGenerator.teacherCode(teacher.getId()));
            teacher = teacherRepository.save(teacher);
            teachers.add(teacher);
        }
        return teachers;
    }

    private void assignFormTeachers(List<SchoolClass> classes, List<Teacher> teachers) {
        for (SchoolClass schoolClass : classes) {
            List<Teacher> assigned = teachers.stream()
                    .filter(t -> t.getClasses().contains(schoolClass))
                    .toList();
            if (!assigned.isEmpty()) {
                schoolClass.setFormTeacher(assigned.get(random.nextInt(assigned.size())));
                schoolClassRepository.save(schoolClass);
            }
        }
    }

    // -------------------------------------------------------------------
    // Students
    // -------------------------------------------------------------------

    private List<Student> seedStudents(List<SchoolClass> classes) {
        List<Student> students = new ArrayList<>();
        for (SchoolClass schoolClass : classes) {
            int classSize = randInt(10, 14);
            for (int i = 0; i < classSize; i++) {
                Gender gender = random.nextDouble() > 0.48 ? Gender.Female : Gender.Male;
                Student student = Student.builder()
                        .studentCode(CodeGenerator.tempCode())
                        .name(fullName(gender))
                        .gender(gender)
                        .schoolClass(schoolClass)
                        .dob(LocalDate.of(randInt(2008, 2014), randInt(1, 9), randInt(1, 28)))
                        .guardianName(fullName(random.nextBoolean() ? Gender.Male : Gender.Female))
                        .guardianPhone("070" + randInt(10000000, 99999999))
                        .address(randInt(1, 200) + " " + pick(STREETS) + ", Lagos")
                        .status(random.nextDouble() > 0.04 ? RecordStatus.Active : RecordStatus.Inactive)
                        .admittedDate(LocalDate.of(randInt(2015, 2024), 9, randInt(1, 9)))
                        .build();
                student = studentRepository.save(student);
                student.setStudentCode(CodeGenerator.studentCode(student.getId()));
                student = studentRepository.save(student);
                students.add(student);
            }
        }
        return students;
    }

    // -------------------------------------------------------------------
    // Results & attendance — baseline ability + per-term drift + noise,
    // so trend charts show plausible gradual improvement/decline rather
    // than pure random noise every term.
    // -------------------------------------------------------------------

    private void seedResultsAndAttendance(List<Student> students, List<AcademicSession> sessions,
                                           List<Term> terms, List<Subject> subjects) {
        java.util.Map<Long, Double> baseline = new java.util.HashMap<>();
        java.util.Map<Long, Double> drift = new java.util.HashMap<>();
        double[] driftOptions = {-6, -3, -1, 0, 0, 1, 2, 4, 6};
        for (Student s : students) {
            baseline.put(s.getId(), randDouble(38, 92));
            drift.put(s.getId(), driftOptions[random.nextInt(driftOptions.length)]);
        }

        int termIndex = 0;
        for (AcademicSession session : sessions) {
            for (Term term : terms) {
                double termDriftFactor = termIndex * 0.6;
                List<Attendance> attendanceBatch = new ArrayList<>();
                List<Result> resultBatch = new ArrayList<>();

                for (Student s : students) {
                    double studentDrift = drift.get(s.getId()) * termDriftFactor;
                    for (Subject subject : subjects) {
                        double noise = randDouble(-9, 9);
                        double pct = Math.max(2, Math.min(100, baseline.get(s.getId()) + studentDrift + noise));
                        int ca = (int) Math.round(Math.min(40, pct * 0.4 + randDouble(-3, 3)));
                        int exam = (int) Math.round(Math.max(0, Math.min(60, pct * 0.6 + randDouble(-4, 4))));
                        ca = Math.max(0, ca);
                        int total = ca + exam;
                        resultBatch.add(Result.builder()
                                .student(s).schoolClass(s.getSchoolClass()).subject(subject)
                                .academicSession(session).term(term)
                                .ca(ca).exam(exam).total(total).grade(Result.gradeFor(total))
                                .build());
                    }

                    double attendanceRateForStudent = randDouble(0.62, 0.99);
                    int days = randInt(18, 24);
                    String monthPrefix = term.getName().equals("First Term") ? "-10-"
                            : term.getName().equals("Second Term") ? "-01-" : "-04-";
                    int yearPart = Integer.parseInt(session.getLabel().split("/")[0]);
                    for (int d = 1; d <= days; d++) {
                        double roll = random.nextDouble();
                        AttendanceStatus status = AttendanceStatus.Present;
                        if (roll > attendanceRateForStudent) {
                            status = random.nextDouble() > 0.35 ? AttendanceStatus.Absent : AttendanceStatus.Late;
                        }
                        attendanceBatch.add(Attendance.builder()
                                .student(s).schoolClass(s.getSchoolClass())
                                .academicSession(session).term(term)
                                .date(LocalDate.parse(yearPart + monthPrefix + String.format("%02d", Math.min(d, 28))))
                                .status(status)
                                .build());
                    }
                }

                resultRepository.saveAll(resultBatch);
                attendanceRepository.saveAll(attendanceBatch);
                termIndex++;
            }
        }
    }

    // -------------------------------------------------------------------
    // Notifications & activity feed
    // -------------------------------------------------------------------

    private void seedNotificationsAndActivity() {
        List<Notification> notifications = List.of(
                Notification.builder().type("attendance_risk")
                        .message("A student's attendance has fallen below 75% this term.")
                        .date(LocalDate.now().minusDays(2)).read(false).build(),
                Notification.builder().type("academic_risk")
                        .message("Several students are below the academic performance threshold this term.")
                        .date(LocalDate.now().minusDays(3)).read(false).build(),
                Notification.builder().type("improvement")
                        .message("A student's performance improved significantly this term.")
                        .date(LocalDate.now().minusDays(4)).read(false).build(),
                Notification.builder().type("system")
                        .message("This term's result upload window closes in 5 days.")
                        .date(LocalDate.now().minusDays(5)).read(true).build(),
                Notification.builder().type("report")
                        .message("A class performance report is ready for download.")
                        .date(LocalDate.now().minusDays(6)).read(true).build());
        notificationRepository.saveAll(notifications);

        List<ActivityLogEntry> activity = List.of(
                ActivityLogEntry.builder().type("performance").message("Second Term scores uploaded.").actor("Admin").build(),
                ActivityLogEntry.builder().type("attendance").message("Attendance recorded for a class.").actor("Admin").build(),
                ActivityLogEntry.builder().type("student").message("A new student was added.").actor("Admin").build(),
                ActivityLogEntry.builder().type("report").message("An at-risk student report was generated.").actor("Admin").build());
        activityLogEntryRepository.saveAll(activity);
    }

    // -------------------------------------------------------------------
    // Accounts
    // -------------------------------------------------------------------

    /**
     * Creates the ADMIN login from app.seed.admin-* (ADMIN_USERNAME /
     * ADMIN_EMAIL / ADMIN_PASSWORD). If a user with that username already
     * exists — e.g. this ever runs against a non-empty admin table — its
     * credentials are updated in place rather than inserting a duplicate.
     */
    private User seedAdmin() {
        User admin = userRepository.findByUsernameIgnoreCase(seedProperties.adminUsername())
                .orElseGet(() -> User.builder().username(seedProperties.adminUsername()).build());
        admin.setEmail(seedProperties.adminEmail());
        admin.setPassword(passwordEncoder.encode(seedProperties.adminPassword()));
        admin.setFullName("Adaobi Chukwu");
        admin.setRole(Role.ADMIN);
        return userRepository.save(admin);
    }

    /** Grants login to exactly one teacher, matching the frontend's original single demo-teacher pattern. */
    private Teacher grantDemoTeacherLogin(List<Teacher> teachers) {
        Teacher demo = teachers.get(2);
        demo.setEmail("teacher@brightfield.edu.ng");
        User user = User.builder()
                .username("teacher")
                .email(demo.getEmail())
                .password(passwordEncoder.encode("teacher123"))
                .fullName(demo.getName())
                .role(Role.TEACHER)
                .build();
        demo.setUser(user);
        teacherRepository.save(demo);
        return demo;
    }

    private void printCredentials(User admin, Teacher demoTeacher) {
        log.info("=================================================================");
        log.info(" DEMO LOGIN CREDENTIALS (development only — do not use in production)");
        log.info("   Administrator — username/email: {} / {}   password: {}", admin.getUsername(), admin.getEmail(),
                seedProperties.adminPassword());
        log.info("   Teacher       — username/email: teacher / {}   password: teacher123", demoTeacher.getEmail());
        log.info("=================================================================");
    }
}
