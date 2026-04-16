package com.myschool.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EnrollmentResponse {
    private Long id;
    private Long studentId;
    private Long courseId;
}