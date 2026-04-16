package com.myschool.userservice.controller;

import com.myschool.userservice.dto.PageResponse;
import com.myschool.userservice.dto.StudentRequest;
import com.myschool.userservice.dto.StudentResponse;
import com.myschool.userservice.entity.Student;
import com.myschool.userservice.entity.User;
import com.myschool.userservice.service.StudentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentResponse create(@RequestBody StudentRequest request) {
        Student s = studentService.create(request);
        return toResponse(s);
    }

    @GetMapping
    public List<StudentResponse> getAll() {
        return studentService.getAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/paged")
    public PageResponse<StudentResponse> getAllPaged(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy
    ) {
        return studentService.getAllPaged(page, size, sortBy);
    }

    @GetMapping("/{id}")
    public StudentResponse getById(@PathVariable Long id) {
        Student s = studentService.getById(id);
        return toResponse(s);
    }

    @PutMapping("/{id}")
    public StudentResponse update(@PathVariable Long id, @RequestBody StudentRequest request) {
        Student s = studentService.update(id, request);
        return toResponse(s);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        studentService.delete(id);
    }

    private StudentResponse toResponse(Student s) {
        User u = s.getUser(); // user contains fullName/email
        return new StudentResponse(
                s.getId(),
                u != null ? u.getFullName() : null,
                u != null ? u.getEmail() : null,
                s.getLevel()
        );
    }
}