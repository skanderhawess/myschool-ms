package com.myschool.userservice.dto;

import lombok.Data;

@Data
public class StudentRequest {

    private String fullName;
    private String email;
    private String password;
    private String level;
}

