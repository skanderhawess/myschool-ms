package com.myschool.userservice.service;

import com.myschool.userservice.dto.CourseStatsDto;
import com.myschool.userservice.dto.DashboardStatsDto;
import com.myschool.userservice.dto.StudentReportDto;
import com.myschool.userservice.entity.Course;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de ReportService.
 *
 * Strictness.LENIENT : le setUp instancie plusieurs fixtures partagees entre
 * tests (Users, Students, Courses, Enrollments). Selon le test, certaines
 * stubs ne sont pas consommees par la methode sous test — c'est un faux positif
 * du mode strict de Mockito. LENIENT neutralise le UnnecessaryStubbingException
 * sans masquer les vraies erreurs d'assertion.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReportServiceTest {

    // -- Mocks ---------------------------------------------------------------

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private ReportService reportService;

    // -- Fixtures ------------------------------------------------------------

    private Student mockStudent1;
    private Student mockStudent2;
    private Course  mockCourse1;   // id=10, "Spring Boot", capacity=2
    private Course  mockCourse2;   // id=20, "Angular",     capacity=2

    @BeforeEach
    void setUp() {
        User mockUser1 = User.builder()
                .id(1L).fullName("Alice Martin").email("alice@test.com").password("pass")
                .build();
        User mockUser2 = User.builder()
                .id(2L).fullName("Bob Dupont").email("bob@test.com").password("pass")
                .build();

        mockStudent1 = Student.builder().id(1L).user(mockUser1).level("1st year").build();
        mockStudent2 = Student.builder().id(2L).user(mockUser2).level("2nd year").build();

        // Capacite=2 pour que les assertions de fillRate (100% et 50%) restent valides :
        //   Spring Boot : 2 inscrits / 2 places = 100%
        //   Angular     : 1 inscrit  / 2 places =  50%
        mockCourse1 = new Course(10L, "Spring Boot", LocalDateTime.now(), 2);
        mockCourse2 = new Course(20L, "Angular",     LocalDateTime.now(), 2);
    }

    /**
     * Helper : stubbe le resultat de countEnrollmentsGroupedByCourse()
     * avec le nouveau contrat List<Object[]> ou chaque ligne = [courseId, count].
     * Cette requete remplace les anciens findAllByCourseId() (optimisation N+1).
     */
    private void stubCourseEnrollmentCounts(long course1Count, long course2Count) {
        when(enrollmentRepository.countEnrollmentsGroupedByCourse()).thenReturn(List.of(
                new Object[]{10L, course1Count},
                new Object[]{20L, course2Count}
        ));
    }

    /**
     * Helper : stubbe le resultat de countEnrollmentsGroupedByStudent()
     * avec le nouveau contrat List<Object[]> ou chaque ligne = [studentId, count].
     */
    private void stubStudentEnrollmentCounts(long student1Count, long student2Count) {
        when(enrollmentRepository.countEnrollmentsGroupedByStudent()).thenReturn(List.of(
                new Object[]{1L, student1Count},
                new Object[]{2L, student2Count}
        ));
    }

    // =======================================================================
    // getDashboardStats()
    // =======================================================================

    @Test
    void getDashboardStats_shouldReturnCorrectCounts() {
        // GIVEN
        when(studentRepository.count()).thenReturn(2L);
        when(courseRepository.count()).thenReturn(2L);
        when(enrollmentRepository.count()).thenReturn(3L);
        when(courseRepository.findAll()).thenReturn(List.of(mockCourse1, mockCourse2));
        stubCourseEnrollmentCounts(2L, 1L);

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
        // GIVEN -- 3 inscriptions / 2 cours => moyenne = 1.5
        when(studentRepository.count()).thenReturn(2L);
        when(courseRepository.count()).thenReturn(2L);
        when(enrollmentRepository.count()).thenReturn(3L);
        when(courseRepository.findAll()).thenReturn(List.of(mockCourse1, mockCourse2));
        stubCourseEnrollmentCounts(2L, 1L);

        // WHEN
        DashboardStatsDto result = reportService.getDashboardStats();

        // THEN
        assertEquals(1.5, result.getAverageEnrollmentsPerCourse());
    }

    @Test
    void getDashboardStats_shouldIdentifyMostPopularCourse() {
        // GIVEN -- Spring Boot a 2 inscriptions, Angular en a 1 => Spring Boot gagne
        when(studentRepository.count()).thenReturn(2L);
        when(courseRepository.count()).thenReturn(2L);
        when(enrollmentRepository.count()).thenReturn(3L);
        when(courseRepository.findAll()).thenReturn(List.of(mockCourse1, mockCourse2));
        stubCourseEnrollmentCounts(2L, 1L);

        // WHEN
        DashboardStatsDto result = reportService.getDashboardStats();

        // THEN
        assertEquals(10L,          result.getMostPopularCourseId());
        assertEquals("Spring Boot", result.getMostPopularCourseTitle());
        assertEquals(2L,           result.getMostPopularCourseEnrollments());
    }

    @Test
    void getDashboardStats_shouldReturnZeroMetrics_whenNoCourses() {
        // GIVEN -- base vide
        when(studentRepository.count()).thenReturn(0L);
        when(courseRepository.count()).thenReturn(0L);
        when(enrollmentRepository.count()).thenReturn(0L);
        when(courseRepository.findAll()).thenReturn(List.of());
        when(enrollmentRepository.countEnrollmentsGroupedByCourse()).thenReturn(List.of());

        // WHEN
        DashboardStatsDto result = reportService.getDashboardStats();

        // THEN -- valeurs par defaut securisees
        assertEquals(0.0,  result.getAverageEnrollmentsPerCourse());
        assertEquals(0L,   result.getMostPopularCourseId());
        assertEquals("N/A", result.getMostPopularCourseTitle());
        assertEquals(0L,   result.getMostPopularCourseEnrollments());
    }

    // =======================================================================
    // getCourseStats()
    // =======================================================================

    @Test
    void getCourseStats_shouldReturnListSortedByEnrollmentCountDescending() {
        // GIVEN -- Spring Boot : 2 inscrits, Angular : 1 inscrit
        when(courseRepository.findAll()).thenReturn(List.of(mockCourse1, mockCourse2));
        stubCourseEnrollmentCounts(2L, 1L);

        // WHEN
        List<CourseStatsDto> result = reportService.getCourseStats();

        // THEN -- Spring Boot doit apparaitre en premier (plus d'inscrits)
        assertEquals(2, result.size());
        assertEquals(10L, result.get(0).getCourseId());
        assertEquals(2L,  result.get(0).getEnrolledCount());
        assertEquals(20L, result.get(1).getCourseId());
        assertEquals(1L,  result.get(1).getEnrolledCount());
    }

    @Test
    void getCourseStats_shouldComputeCorrectFillRates() {
        // GIVEN -- capacite=2 par cours (voir setUp) :
        //   Spring Boot : 2 inscrits / 2 places = 100.0 %
        //   Angular     : 1 inscrit  / 2 places =  50.0 %
        when(courseRepository.findAll()).thenReturn(List.of(mockCourse1, mockCourse2));
        stubCourseEnrollmentCounts(2L, 1L);

        // WHEN
        List<CourseStatsDto> result = reportService.getCourseStats();

        // THEN -- liste triee desc : index 0 = Spring Boot, index 1 = Angular
        assertEquals(100.0, result.get(0).getFillRate());
        assertEquals(50.0,  result.get(1).getFillRate());
    }

    @Test
    void getCourseStats_shouldReturnEmptyList_whenNoCourses() {
        // GIVEN
        when(courseRepository.findAll()).thenReturn(List.of());
        when(enrollmentRepository.countEnrollmentsGroupedByCourse()).thenReturn(List.of());

        // WHEN
        List<CourseStatsDto> result = reportService.getCourseStats();

        // THEN
        assertTrue(result.isEmpty());
    }

    // =======================================================================
    // getStudentReport()
    // =======================================================================

    @Test
    void getStudentReport_shouldReturnAllStudentsWithCorrectEnrollmentCounts() {
        // GIVEN -- Alice inscrite a 2 cours, Bob inscrit a 1
        when(studentRepository.findAll()).thenReturn(List.of(mockStudent1, mockStudent2));
        stubStudentEnrollmentCounts(2L, 1L);

        // WHEN
        List<StudentReportDto> result = reportService.getStudentReport();

        // THEN -- tri alphabetique : Alice avant Bob
        assertEquals(2, result.size());
        assertEquals(2L, result.get(0).getEnrolledCourses()); // Alice Martin
        assertEquals(1L, result.get(1).getEnrolledCourses()); // Bob Dupont
    }

    @Test
    void getStudentReport_shouldBeSortedAlphabeticallyByFullName() {
        // GIVEN -- ordre d'entree volontairement inverse pour verifier le tri
        when(studentRepository.findAll()).thenReturn(List.of(mockStudent2, mockStudent1));
        stubStudentEnrollmentCounts(2L, 1L);

        // WHEN
        List<StudentReportDto> result = reportService.getStudentReport();

        // THEN -- sortie triee alphabetiquement quel que soit l'ordre d'entree
        assertEquals("Alice Martin", result.get(0).getFullName());
        assertEquals("Bob Dupont",   result.get(1).getFullName());
    }

    @Test
    void getStudentReport_shouldReturnEmptyList_whenNoStudents() {
        // GIVEN
        when(studentRepository.findAll()).thenReturn(List.of());
        when(enrollmentRepository.countEnrollmentsGroupedByStudent()).thenReturn(List.of());

        // WHEN
        List<StudentReportDto> result = reportService.getStudentReport();

        // THEN
        assertTrue(result.isEmpty());
    }
}
