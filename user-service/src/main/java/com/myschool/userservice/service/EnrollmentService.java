package com.myschool.userservice.service;

import com.myschool.userservice.client.NotificationClient;
import com.myschool.userservice.client.dto.NotificationRequestDto;
import com.myschool.userservice.dto.EnrollmentRequest;
import com.myschool.userservice.dto.EnrollmentResponse;
import com.myschool.userservice.entity.Course;
import com.myschool.userservice.entity.Enrollment;
import com.myschool.userservice.entity.Student;
import com.myschool.userservice.repository.CourseRepository;
import com.myschool.userservice.repository.EnrollmentRepository;
import com.myschool.userservice.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final NotificationClient notificationClient;

    @Value("${app.admin.email:shawess618@gmail.com}")
    private String adminEmail;

    @Transactional
    public EnrollmentResponse enroll(EnrollmentRequest req) {
        if (!studentRepository.existsById(req.getStudentId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found");
        }

        if (enrollmentRepository.existsByStudentIdAndCourseId(req.getStudentId(), req.getCourseId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Student already enrolled in this course");
        }

        Enrollment saved = enrollmentRepository.save(
                Enrollment.builder()
                        .studentId(req.getStudentId())
                        .courseId(req.getCourseId())
                        .build()
        );

        Course course = courseRepository.findById(req.getCourseId()).orElse(null);
        Student student = studentRepository.findById(req.getStudentId()).orElse(null);

        if (course != null) {
            String courseDate = course.getDateTime() != null
                    ? course.getDateTime().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))
                    : "TBD";

            // Scénario 2 : confirmation d'inscription via Feign
            if (student != null && student.getUser() != null) {
                try {
                    NotificationRequestDto notifRequest = NotificationRequestDto.builder()
                            .recipientEmail(student.getUser().getEmail())
                            .recipientId(student.getId())
                            .subject("Enrollment Confirmed — " + course.getTitle())
                            .type(NotificationRequestDto.NotificationType.ENROLLMENT_CONFIRMATION)
                            .studentName(student.getUser().getFullName())
                            .courseTitle(course.getTitle())
                            .courseDate(courseDate)
                            .build();
                    notificationClient.sendNotification(notifRequest);
                    log.info("Enrollment notification sent via Feign to {}", student.getUser().getEmail());
                } catch (Exception e) {
                    log.error("Feign call failed for enrollment notif", e);
                }
            }

            // Scénario 3 : alerte capacité via Feign
            int maxCap = course.getMaxCapacity() != null && course.getMaxCapacity() > 0
                    ? course.getMaxCapacity() : 5;
            long enrolledCount = enrollmentRepository.findAllByCourseId(req.getCourseId()).size();
            if (enrolledCount >= maxCap) {
                try {
                    NotificationRequestDto alertRequest = NotificationRequestDto.builder()
                            .recipientEmail(adminEmail)
                            .subject("Course Full: " + course.getTitle())
                            .type(NotificationRequestDto.NotificationType.CAPACITY_ALERT)
                            .courseTitle(course.getTitle())
                            .courseDate(courseDate)
                            .enrolledCount((int) enrolledCount)
                            .maxCapacity(maxCap)
                            .build();
                    notificationClient.sendNotification(alertRequest);
                    log.info("Capacity alert sent via Feign for course {}", course.getTitle());
                } catch (Exception e) {
                    log.error("Feign call failed for capacity alert", e);
                }
            }
        }

        return toResponse(saved);
    }

    public List<EnrollmentResponse> findAll() {
        return enrollmentRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public EnrollmentResponse findById(Long id) {
        Enrollment e = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enrollment not found"));
        return toResponse(e);
    }

    public List<EnrollmentResponse> findByStudentId(Long studentId) {
        return enrollmentRepository.findAllByStudentId(studentId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public EnrollmentResponse update(Long id, EnrollmentRequest req) {
        Enrollment existing = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Enrollment not found"));

        if (!studentRepository.existsById(req.getStudentId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found");
        }

        boolean samePair = existing.getStudentId().equals(req.getStudentId())
                && existing.getCourseId().equals(req.getCourseId());

        if (!samePair && enrollmentRepository.existsByStudentIdAndCourseId(req.getStudentId(), req.getCourseId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Student already enrolled in this course");
        }

        existing.setStudentId(req.getStudentId());
        existing.setCourseId(req.getCourseId());

        return toResponse(enrollmentRepository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        if (!enrollmentRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Enrollment not found");
        }
        enrollmentRepository.deleteById(id);
    }

    private EnrollmentResponse toResponse(Enrollment e) {
        return new EnrollmentResponse(e.getId(), e.getStudentId(), e.getCourseId());
    }
}
