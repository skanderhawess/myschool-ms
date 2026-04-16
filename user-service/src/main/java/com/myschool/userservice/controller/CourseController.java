package com.myschool.userservice.controller;

import com.myschool.userservice.dto.CourseRequest;
import com.myschool.userservice.dto.CourseResponse;
import com.myschool.userservice.dto.PageResponse;
import com.myschool.userservice.service.CoursesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CoursesService service;

    @GetMapping
    public List<CourseResponse> getAll() { return service.getAll(); }

    @GetMapping("/paged")
    public PageResponse<CourseResponse> getAllPaged(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy
    ) {
        return service.getAllPaged(page, size, sortBy);
    }

    @GetMapping("/{id}")
    public CourseResponse getById(@PathVariable Long id) { return service.getById(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponse create(@Valid @RequestBody CourseRequest req) { return service.create(req); }

    @PutMapping("/{id}")
    public CourseResponse update(@PathVariable Long id, @Valid @RequestBody CourseRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }
}