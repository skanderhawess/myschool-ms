package com.myschool.userservice.service;

import com.myschool.userservice.client.NotificationClient;
import com.myschool.userservice.client.dto.NotificationRequestDto;
import com.myschool.userservice.dto.PageResponse;
import com.myschool.userservice.dto.StudentRequest;
import com.myschool.userservice.dto.StudentResponse;
import com.myschool.userservice.entity.Student;
import com.myschool.userservice.entity.User;
import com.myschool.userservice.repository.EnrollmentRepository;
import com.myschool.userservice.repository.StudentRepository;
import com.myschool.userservice.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EnrollmentRepository enrollmentRepository;
    private final NotificationClient notificationClient;

    public StudentService(StudentRepository studentRepository,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          EnrollmentRepository enrollmentRepository,
                          NotificationClient notificationClient) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.enrollmentRepository = enrollmentRepository;
        this.notificationClient = notificationClient;
    }

    @Transactional
    public Student create(StudentRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        // 1) Create user first
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        user = userRepository.save(user);

        // 2) Create student linked to user (shared PK via @MapsId)
        Student student = Student.builder()
                .user(user)
                .level(request.getLevel())
                .build();

        Student saved = studentRepository.save(student);

        // Envoi de la notification de bienvenue via Feign (notification-service)
        try {
            NotificationRequestDto notifRequest = NotificationRequestDto.builder()
                    .recipientEmail(request.getEmail())
                    .recipientId(saved.getId())
                    .subject("Welcome to MySchool")
                    .type(NotificationRequestDto.NotificationType.WELCOME_EMAIL)
                    .studentName(request.getFullName())
                    .build();
            notificationClient.sendNotification(notifRequest);
            log.info("Welcome notification sent via Feign to {}", request.getEmail());
        } catch (Exception e) {
            // Ne pas bloquer la création si la notification échoue
            log.error("Feign call to notification-service failed for {}", request.getEmail(), e);
        }

        return saved;
    }

    public List<Student> getAll() {
        return studentRepository.findAll();
    }

    public PageResponse<StudentResponse> getAllPaged(int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());
        Page<Student> studentPage = studentRepository.findAll(pageable);

        List<StudentResponse> content = studentPage.getContent()
                .stream()
                .map(s -> new StudentResponse(
                        s.getId(),
                        s.getUser() != null ? s.getUser().getFullName() : null,
                        s.getUser() != null ? s.getUser().getEmail() : null,
                        s.getLevel()
                ))
                .collect(Collectors.toList());

        return new PageResponse<>(
                content,
                studentPage.getNumber(),
                studentPage.getSize(),
                studentPage.getTotalElements(),
                studentPage.getTotalPages(),
                studentPage.isLast()
        );
    }

    public Student getById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found"));
    }

    @Transactional
    public Student update(Long id, StudentRequest request) {
        Student existing = getById(id);

        User user = existing.getUser();
        if (user == null) {
            // Should never happen if DB is consistent, but prevents NullPointerException
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Student user relation is missing");
        }

        // Email uniqueness check
        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        // Update user fields
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        // Update student-only fields
        existing.setLevel(request.getLevel());

        // Save user first, then student (safe)
        userRepository.save(user);
        return studentRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        Student student = getById(id);

        // delete enrollments first to avoid FK violations
        enrollmentRepository.deleteAllByStudentId(id);

        // delete student row
        studentRepository.delete(student);

        // delete user row (if FK ON DELETE CASCADE exists, this may already be handled)
        if (student.getUser() != null) {
            userRepository.deleteById(student.getUser().getId());
        }
    }
}
