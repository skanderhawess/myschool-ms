package com.myschool.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StudentResponse {

    private Long id;
    private String fullName;
    private String email;
    private String level;
}

