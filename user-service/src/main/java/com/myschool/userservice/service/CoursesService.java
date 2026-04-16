package com.myschool.userservice.service;

import com.myschool.userservice.dto.CourseRequest;
import com.myschool.userservice.dto.CourseResponse;
import com.myschool.userservice.dto.PageResponse;
import com.myschool.userservice.entity.Course;
import com.myschool.userservice.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CoursesService {

    private final CourseRepository repo;

    public List<CourseResponse> getAll() {
        return repo.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public PageResponse<CourseResponse> getAllPaged(int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());
        Page<Course> coursePage = repo.findAll(pageable);

        List<CourseResponse> content = coursePage.getContent()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return new PageResponse<>(
                content,
                coursePage.getNumber(),
                coursePage.getSize(),
                coursePage.getTotalElements(),
                coursePage.getTotalPages(),
                coursePage.isLast()
        );
    }

    public CourseResponse getById(Long id) {
        Course c = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        return toResponse(c);
    }

    public CourseResponse create(CourseRequest req) {
        Course c = new Course();
        c.setTitle(req.getTitle().trim());
        c.setDateTime(req.getDateTime() != null ? req.getDateTime() : LocalDateTime.now());
        c.setMaxCapacity(req.getMaxCapacity() != null ? req.getMaxCapacity() : 30);
        return toResponse(repo.save(c));
    }

    public CourseResponse update(Long id, CourseRequest req) {
        Course c = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        c.setTitle(req.getTitle().trim());
        if (req.getDateTime() != null) c.setDateTime(req.getDateTime());
        if (req.getMaxCapacity() != null) c.setMaxCapacity(req.getMaxCapacity());

        return toResponse(repo.save(c));
    }

    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
        }
        repo.deleteById(id);
    }

    private CourseResponse toResponse(Course c) {
        return new CourseResponse(c.getId(), c.getTitle(), c.getDateTime(), c.getMaxCapacity());
    }
}