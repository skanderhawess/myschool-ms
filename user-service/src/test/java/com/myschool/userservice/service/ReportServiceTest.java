package com.myschool.userservice.service;

import com.myschool.userservice.dto.CourseStatsDto;
import com.myschool.userservice.dto.DashboardStatsDto;
import com.myschool.userservice.dto.StudentReportDto;
import com.myschool.userservice.entity.Course;
import com.myschool.userservice.entity.Enrollment;
import com.myschool.userservice.entity.Student;
import com.myschool.userservice.entity.User;
import com.myschool.userservice.repository.CourseRepository;
import com.myschool.userservice.repository.EnrollmentRepository;
import com.myschool.userservice.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    // ── Mocks ──────────────────────────────────────────────────────────────

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private ReportService reportService;

    // ── Fixtures ───────────────────────────────────────────────────────────

    private User       mockUser1;
    private User       mockUser2;
    private Student    mockStudent1;
    private Student    mockStudent2;
    private Course     mockCourse1;   // id=10, "Spring Boot"
    private Course     mockCourse2;   // id=20, "Angular"
    private Enrollment enrollment1;   // student 1 → course 10
    private Enrollment enrollment2;   // student 1 → course 20
    private Enrollment enrollment3;   // student 2 → course 10

    @BeforeEach
    void setUp() {
        mockUser1 = User.builder()
                .id(1L).fullName("Alice Martin").email("alice@test.com").password("pass")
                .build();
        mockUser2 = User.builder()
                .id(2L).fullName("Bob Dupont").email("bob@test.com").password("pass")
                .build();

        mockStudent1 = Student.builder().id(1L).user(mockUser1).level("1st year").build();
        mockStudent2 = Student.builder().id(2L).user(mockUser2).level("2nd year").build();

        // Course has @AllArgsConstructor (no @Builder) → use constructor
        mockCourse1 = new Course(10L, "Spring Boot", LocalDateTime.now(),30);
        mockCourse2 = new Course(20L, "Angular",     LocalDateTime.now(),30);

        enrollment1 = Enrollment.builder().id(1L).studentId(1L).courseId(10L).build();
        enrollment2 = Enrollment.builder().id(2L).studentId(1L).courseId(20L).build();
        enrollment3 = Enrollment.builder().id(3L).studentId(2L).courseId(10L).build();
    }

    // ══════════════════════════════════════════════════════════════════════
    // getDashboardStats()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void getDashboardStats_shouldReturnCorrectCounts() {
        // GIVEN
        when(studentRepository.count()).thenReturn(2L);
        when(courseRepository.count()).thenReturn(2L);
        when(enrollmentRepository.count()).thenReturn(3L);
        when(courseRepository.findAll()).thenReturn(List.of(mockCourse1, mockCourse2));
        when(enrollmentRepository.findAllByCourseId(10L)).thenReturn(List.of(enrollment1, enrollment3));
        when(enrollmentRepository.findAllByCourseId(20L)).thenReturn(List.of(enrollment2));

        // WHEN
        DashboardStatsDto result = reportService.getDashboardStats();

        // THEN
        assertNotNull(result);
        assertEquals(2L, result.getTotalStudents());
        assertEquals(2L, result.getTotalCourses());
        assertEquals(3L, result.getTotalEnrollments());
    }

    @Test
    void getDashboardStats_shouldComputeCorrectAverageEnrollmentsPerCourse() {
        // GIVEN — 3 enrollments / 2 courses → avg = 1.5
        when(studentRepository.count()).thenReturn(2L);
        when(courseRepository.count()).thenReturn(2L);
        when(enrollmentRepository.count()).thenReturn(3L);
        when(courseRepository.findAll()).thenReturn(List.of(mockCourse1, mockCourse2));
        when(enrollmentRepository.findAllByCourseId(10L)).thenReturn(List.of(enrollment1, enrollment3));
        when(enrollmentRepository.findAllByCourseId(20L)).thenReturn(List.of(enrollment2));

        // WHEN
        DashboardStatsDto result = reportService.getDashboardStats();

        // THEN
        assertEquals(1.5, result.getAverageEnrollmentsPerCourse());
    }

    @Test
    void getDashboardStats_shouldIdentifyMostPopularCourse() {
        // GIVEN — Spring Boot has 2 enrollments, Angular has 1 → Spring Boot wins
        when(studentRepository.count()).thenReturn(2L);
        when(courseRepository.count()).thenReturn(2L);
        when(enrollmentRepository.count()).thenReturn(3L);
        when(courseRepository.findAll()).thenReturn(List.of(mockCourse1, mockCourse2));
        when(enrollmentRepository.findAllByCourseId(10L)).thenReturn(List.of(enrollment1, enrollment3));
        when(enrollmentRepository.findAllByCourseId(20L)).thenReturn(List.of(enrollment2));

        // WHEN
        DashboardStatsDto result = reportService.getDashboardStats();

        // THEN
        assertEquals(10L,          result.getMostPopularCourseId());
        assertEquals("Spring Boot", result.getMostPopularCourseTitle());
        assertEquals(2L,           result.getMostPopularCourseEnrollments());
    }

    @Test
    void getDashboardStats_shouldReturnZeroMetrics_whenNoCourses() {
        // GIVEN — database is empty
        when(studentRepository.count()).thenReturn(0L);
        when(courseRepository.count()).thenReturn(0L);
        when(enrollmentRepository.count()).thenReturn(0L);
        when(courseRepository.findAll()).thenReturn(List.of());
        // findAllByCourseId is never reached (empty stream) → no stub needed

        // WHEN
        DashboardStatsDto result = reportService.getDashboardStats();

        // THEN — safe defaults when there are no courses
        assertEquals(0.0,  result.getAverageEnrollmentsPerCourse());
        assertEquals(0L,   result.getMostPopularCourseId());
        assertEquals("N/A", result.getMostPopularCourseTitle());
        assertEquals(0L,   result.getMostPopularCourseEnrollments());
    }

    // ══════════════════════════════════════════════════════════════════════
    // getCourseStats()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void getCourseStats_shouldReturnListSortedByEnrollmentCountDescending() {
        // GIVEN — Spring Boot 2 enrolled, Angular 1 enrolled
        when(studentRepository.count()).thenReturn(2L);
        when(courseRepository.findAll()).thenReturn(List.of(mockCourse1, mockCourse2));
        when(enrollmentRepository.findAllByCourseId(10L)).thenReturn(List.of(enrollment1, enrollment3));
        when(enrollmentRepository.findAllByCourseId(20L)).thenReturn(List.of(enrollment2));

        // WHEN
        List<CourseStatsDto> result = reportService.getCourseStats();

        // THEN — Spring Boot must appear first (most enrolled)
        assertEquals(2, result.size());
        assertEquals(10L, result.get(0).getCourseId());
        assertEquals(2L,  result.get(0).getEnrolledCount());
        assertEquals(20L, result.get(1).getCourseId());
        assertEquals(1L,  result.get(1).getEnrolledCount());
    }

    @Test
    void getCourseStats_shouldComputeCorrectFillRates() {
        // GIVEN — 2 total students
        //   Spring Boot: 2 enrolled → 2/2 * 100 = 100.0 %
        //   Angular:     1 enrolled → 1/2 * 100 =  50.0 %
        when(studentRepository.count()).thenReturn(2L);
        when(courseRepository.findAll()).thenReturn(List.of(mockCourse1, mockCourse2));
        when(enrollmentRepository.findAllByCourseId(10L)).thenReturn(List.of(enrollment1, enrollment3));
        when(enrollmentRepository.findAllByCourseId(20L)).thenReturn(List.of(enrollment2));

        // WHEN
        List<CourseStatsDto> result = reportService.getCourseStats();

        // THEN — result is sorted desc so index 0 = Spring Boot, index 1 = Angular
        assertEquals(100.0, result.get(0).getFillRate());
        assertEquals(50.0,  result.get(1).getFillRate());
    }

    @Test
    void getCourseStats_shouldReturnEmptyList_whenNoCourses() {
        // GIVEN
        when(studentRepository.count()).thenReturn(0L);
        when(courseRepository.findAll()).thenReturn(List.of());
        // findAllByCourseId is never reached → no stub needed

        // WHEN
        List<CourseStatsDto> result = reportService.getCourseStats();

        // THEN
        assertTrue(result.isEmpty());
    }

    // ══════════════════════════════════════════════════════════════════════
    // getStudentReport()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void getStudentReport_shouldReturnAllStudentsWithCorrectEnrollmentCounts() {
        // GIVEN — Alice enrolled in 2 courses, Bob enrolled in 1
        when(studentRepository.findAll()).thenReturn(List.of(mockStudent1, mockStudent2));
        when(enrollmentRepository.findAllByStudentId(1L)).thenReturn(List.of(enrollment1, enrollment2));
        when(enrollmentRepository.findAllByStudentId(2L)).thenReturn(List.of(enrollment3));

        // WHEN
        List<StudentReportDto> result = reportService.getStudentReport();

        // THEN — sorted alphabetically: Alice before Bob
        assertEquals(2, result.size());
        assertEquals(2L, result.get(0).getEnrolledCourses()); // Alice Martin
        assertEquals(1L, result.get(1).getEnrolledCourses()); // Bob Dupont
    }

    @Test
    void getStudentReport_shouldBeSortedAlphabeticallyByFullName() {
        // GIVEN — input list is intentionally reversed to verify sorting
        when(studentRepository.findAll()).thenReturn(List.of(mockStudent2, mockStudent1));
        when(enrollmentRepository.findAllByStudentId(1L)).thenReturn(List.of(enrollment1, enrollment2));
        when(enrollmentRepository.findAllByStudentId(2L)).thenReturn(List.of(enrollment3));

        // WHEN
        List<StudentReportDto> result = reportService.getStudentReport();

        // THEN — output must be alphabetical regardless of input order
        assertEquals("Alice Martin", result.get(0).getFullName());
        assertEquals("Bob Dupont",   result.get(1).getFullName());
    }

    @Test
    void getStudentReport_shouldReturnEmptyList_whenNoStudents() {
        // GIVEN
        when(studentRepository.findAll()).thenReturn(List.of());
        // findAllByStudentId is never reached → no stub needed

        // WHEN
        List<StudentReportDto> result = reportService.getStudentReport();

        // THEN
        assertTrue(result.isEmpty());
    }
}
