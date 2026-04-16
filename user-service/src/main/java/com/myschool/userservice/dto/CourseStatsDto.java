package com.myschool.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CourseStatsDto {
    private Long courseId;
    private String courseTitle;
    private long enrolledCount;
    private double fillRate;
    private Integer maxCapacity;
}
