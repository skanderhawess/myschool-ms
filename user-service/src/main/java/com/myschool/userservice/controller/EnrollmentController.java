package com.myschool.userservice.controller;

import com.myschool.userservice.dto.EnrollmentRequest;
import com.myschool.userservice.dto.EnrollmentResponse;
import com.myschool.userservice.service.EnrollmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentResponse create(@Valid @RequestBody EnrollmentRequest req) {
        return enrollmentService.enroll(req);
    }

    @GetMapping
    public List<EnrollmentResponse> findAll() {
        return enrollmentService.findAll();
    }

    @GetMapping("/{id}")
    public EnrollmentResponse findById(@PathVariable Long id) {
        return enrollmentService.findById(id);
    }

    @GetMapping("/student/{studentId}")
    public List<EnrollmentResponse> findByStudent(@PathVariable Long studentId) {
        return enrollmentService.findByStudentId(studentId);
    }

    @PutMapping("/{id}")
    public EnrollmentResponse update(@PathVariable Long id,
                                     @Valid @RequestBody EnrollmentRequest req) {
        return enrollmentService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        enrollmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
