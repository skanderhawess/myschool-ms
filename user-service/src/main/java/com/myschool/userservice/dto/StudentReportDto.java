package com.myschool.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentReportDto {
    private Long studentId;
    private String fullName;
    private String email;
    private String level;
    private long enrolledCourses;
}
