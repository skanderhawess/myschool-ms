package com.myschool.userservice.service;

import com.myschool.userservice.client.NotificationClient;
import com.myschool.userservice.dto.EnrollmentRequest;
import com.myschool.userservice.dto.EnrollmentResponse;
import com.myschool.userservice.entity.Enrollment;
import com.myschool.userservice.repository.CourseRepository;
import com.myschool.userservice.repository.EnrollmentRepository;
import com.myschool.userservice.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    // ── Mocks ──────────────────────────────────────────────────────────────

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private StudentRepository studentRepository;

    // Dependances supplementaires de EnrollmentService :
    //   - CourseRepository  : utilise dans enroll() pour recuperer le cours et
    //     alimenter la notification Feign. Sans ce @Mock, courseRepository est
    //     null et provoque un NullPointerException.
    //   - NotificationClient : client Feign appele apres la sauvegarde. Sans
    //     ce @Mock, l'injection Mockito echouerait (constructeur @RequiredArgsConstructor).
    @Mock
    private CourseRepository courseRepository;

    @Mock
    private NotificationClient notificationClient;

    @InjectMocks
    private EnrollmentService enrollmentService;

    // ── Fixtures ───────────────────────────────────────────────────────────

    private EnrollmentRequest mockRequest;
    private Enrollment        mockEnrollment;

    @BeforeEach
    void setUp() {
        mockRequest = new EnrollmentRequest();
        mockRequest.setStudentId(1L);
        mockRequest.setCourseId(10L);

        mockEnrollment = Enrollment.builder()
                .id(1L)
                .studentId(1L)
                .courseId(10L)
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════
    // enroll()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void enroll_shouldThrowNotFound_whenStudentNotExists() {
        // GIVEN — no student with id 1 in the database
        when(studentRepository.existsById(1L)).thenReturn(false);

        // WHEN
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> enrollmentService.enroll(mockRequest)
        );

        // THEN
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void enroll_shouldThrowConflict_whenAlreadyEnrolled() {
        // GIVEN — student exists but is already enrolled in this course
        when(studentRepository.existsById(1L)).thenReturn(true);
        when(enrollmentRepository.existsByStudentIdAndCourseId(1L, 10L)).thenReturn(true);

        // WHEN
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> enrollmentService.enroll(mockRequest)
        );

        // THEN
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void enroll_shouldSaveAndReturnResponse_whenValid() {
        // GIVEN — student exists and is not yet enrolled
        when(studentRepository.existsById(1L)).thenReturn(true);
        when(enrollmentRepository.existsByStudentIdAndCourseId(1L, 10L)).thenReturn(false);
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(mockEnrollment);

        // WHEN
        EnrollmentResponse result = enrollmentService.enroll(mockRequest);

        // THEN
        assertNotNull(result);
        assertEquals(1L,  result.getStudentId());
        assertEquals(10L, result.getCourseId());
        verify(enrollmentRepository, times(1)).save(any(Enrollment.class));
    }

    // ══════════════════════════════════════════════════════════════════════
    // findById()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void findById_shouldReturnResponse_whenExists() {
        // GIVEN
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(mockEnrollment));

        // WHEN
        EnrollmentResponse result = enrollmentService.findById(1L);

        // THEN
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void findById_shouldThrowNotFound_whenNotExists() {
        // GIVEN
        when(enrollmentRepository.findById(99L)).thenReturn(Optional.empty());

        // WHEN
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> enrollmentService.findById(99L)
        );

        // THEN
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // ══════════════════════════════════════════════════════════════════════
    // findByStudentId()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void findByStudentId_shouldReturnList_whenStudentHasEnrollments() {
        // GIVEN
        when(enrollmentRepository.findAllByStudentId(1L)).thenReturn(List.of(mockEnrollment));

        // WHEN
        List<EnrollmentResponse> result = enrollmentService.findByStudentId(1L);

        // THEN
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getStudentId());
    }

    // ══════════════════════════════════════════════════════════════════════
    // update()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void update_shouldThrowNotFound_whenEnrollmentNotExists() {
        // GIVEN
        when(enrollmentRepository.findById(99L)).thenReturn(Optional.empty());

        // WHEN
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> enrollmentService.update(99L, mockRequest)
        );

        // THEN
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void update_shouldThrowConflict_whenNewPairAlreadyExists() {
        // GIVEN — existing enrollment is (studentId=1, courseId=10)
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(mockEnrollment));
        when(studentRepository.existsById(1L)).thenReturn(true);

        // Request targets a DIFFERENT courseId → samePair = false → conflict check runs
        mockRequest.setCourseId(20L);
        when(enrollmentRepository.existsByStudentIdAndCourseId(1L, 20L)).thenReturn(true);

        // WHEN
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> enrollmentService.update(1L, mockRequest)
        );

        // THEN
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void update_shouldSaveAndReturn_whenValid() {
        // GIVEN — request has the SAME studentId + courseId as the existing enrollment
        // samePair = true → existsByStudentIdAndCourseId is never called (short-circuit)
        when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(mockEnrollment));
        when(studentRepository.existsById(1L)).thenReturn(true);
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(mockEnrollment);

        // WHEN
        EnrollmentResponse result = enrollmentService.update(1L, mockRequest);

        // THEN
        assertNotNull(result);
        verify(enrollmentRepository, times(1)).save(any(Enrollment.class));
    }

    // ══════════════════════════════════════════════════════════════════════
    // delete()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void delete_shouldDeleteSuccessfully_whenExists() {
        // GIVEN
        when(enrollmentRepository.existsById(1L)).thenReturn(true);

        // WHEN
        assertDoesNotThrow(() -> enrollmentService.delete(1L));

        // THEN
        verify(enrollmentRepository, times(1)).deleteById(1L);
    }

    @Test
    void delete_shouldThrowNotFound_whenNotExists() {
        // GIVEN
        when(enrollmentRepository.existsById(99L)).thenReturn(false);

        // WHEN
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> enrollmentService.delete(99L)
        );

        // THEN
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(enrollmentRepository, never()).deleteById(any());
    }
}
