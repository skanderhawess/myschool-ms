package com.myschool.userservice.service;

import com.myschool.userservice.dto.StudentRequest;
import com.myschool.userservice.entity.Student;
import com.myschool.userservice.entity.User;
import com.myschool.userservice.repository.EnrollmentRepository;
import com.myschool.userservice.repository.StudentRepository;
import com.myschool.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import org.mockito.InOrder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    // ── Mocks ──────────────────────────────────────────────────────────────

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private StudentService studentService;

    // ── Fixtures ───────────────────────────────────────────────────────────

    private User mockUser;
    private Student mockStudent;
    private StudentRequest mockRequest;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .fullName("John Doe")
                .email("john@test.com")
                .password("encoded_password")
                .build();

        mockStudent = Student.builder()
                .id(1L)
                .user(mockUser)
                .level("1st year")
                .build();

        mockRequest = new StudentRequest();
        mockRequest.setFullName("John Doe");
        mockRequest.setEmail("john@test.com");
        mockRequest.setPassword("password123");
        mockRequest.setLevel("1st year");
    }

    // ══════════════════════════════════════════════════════════════════════
    // create()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void create_shouldThrowConflict_whenEmailAlreadyExists() {
        // GIVEN
        when(userRepository.existsByEmail("john@test.com")).thenReturn(true);

        // WHEN
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> studentService.create(mockRequest)
        );

        // THEN
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(userRepository, never()).save(any());
        verify(studentRepository, never()).save(any());
    }

    @Test
    void create_shouldSaveUserAndStudent_whenEmailIsNew() {
        // GIVEN
        when(userRepository.existsByEmail("john@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(studentRepository.save(any(Student.class))).thenReturn(mockStudent);

        // WHEN
        Student result = studentService.create(mockRequest);

        // THEN
        assertNotNull(result);
        verify(userRepository, times(1)).save(any(User.class));
        verify(studentRepository, times(1)).save(any(Student.class));
    }

    @Test
    void create_shouldEncodePassword_beforeSaving() {
        // GIVEN
        when(userRepository.existsByEmail("john@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(studentRepository.save(any(Student.class))).thenReturn(mockStudent);

        // WHEN
        studentService.create(mockRequest);

        // THEN — password must be encoded before the User is persisted
        verify(passwordEncoder, times(1)).encode("password123");
    }

    // ══════════════════════════════════════════════════════════════════════
    // getById()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void getById_shouldReturnStudent_whenExists() {
        // GIVEN
        when(studentRepository.findById(1L)).thenReturn(Optional.of(mockStudent));

        // WHEN
        Student result = studentService.getById(1L);

        // THEN
        assertEquals(mockStudent, result);
    }

    @Test
    void getById_shouldThrowNotFound_whenNotExists() {
        // GIVEN
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        // WHEN
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> studentService.getById(99L)
        );

        // THEN
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    // ══════════════════════════════════════════════════════════════════════
    // update()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void update_shouldThrowNotFound_whenStudentNotExists() {
        // GIVEN
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        // WHEN
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> studentService.update(99L, mockRequest)
        );

        // THEN
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void update_shouldThrowConflict_whenEmailTakenByOtherUser() {
        // GIVEN — existing student has email "john@test.com"
        when(studentRepository.findById(1L)).thenReturn(Optional.of(mockStudent));

        // Request uses a NEW email (different → triggers uniqueness check)
        mockRequest.setEmail("other@test.com");
        when(userRepository.existsByEmail("other@test.com")).thenReturn(true);

        // WHEN
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> studentService.update(1L, mockRequest)
        );

        // THEN
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(userRepository, never()).save(any());
        verify(studentRepository, never()).save(any());
    }

    @Test
    void update_shouldSaveChanges_whenValid() {
        // GIVEN — email in request matches existing email → uniqueness check is skipped
        when(studentRepository.findById(1L)).thenReturn(Optional.of(mockStudent));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(studentRepository.save(any(Student.class))).thenReturn(mockStudent);

        // WHEN
        Student result = studentService.update(1L, mockRequest);

        // THEN
        assertNotNull(result);
        verify(userRepository, times(1)).save(any(User.class));
        verify(studentRepository, times(1)).save(any(Student.class));
    }

    // ══════════════════════════════════════════════════════════════════════
    // delete()
    // ══════════════════════════════════════════════════════════════════════

    @Test
    void delete_shouldDeleteEnrollmentsFirst_beforeDeletingStudent() {
        // GIVEN
        when(studentRepository.findById(1L)).thenReturn(Optional.of(mockStudent));

        // WHEN
        studentService.delete(1L);

        // THEN — deletion order: enrollments → student → user
        InOrder order = inOrder(enrollmentRepository, studentRepository, userRepository);
        order.verify(enrollmentRepository).deleteAllByStudentId(1L);
        order.verify(studentRepository).delete(mockStudent);
        order.verify(userRepository).deleteById(1L);
    }

    @Test
    void delete_shouldThrowNotFound_whenStudentNotExists() {
        // GIVEN
        when(studentRepository.findById(99L)).thenReturn(Optional.empty());

        // WHEN
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> studentService.delete(99L)
        );

        // THEN
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(enrollmentRepository, never()).deleteAllByStudentId(any());
        verify(studentRepository, never()).delete(any(Student.class));
    }
}
