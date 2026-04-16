package com.myschool.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {
    private long totalStudents;
    private long totalCourses;
    private long totalEnrollments;
    private double averageEnrollmentsPerCourse;
    private long mostPopularCourseId;
    private String mostPopularCourseTitle;
    private long mostPopularCourseEnrollments;
}
