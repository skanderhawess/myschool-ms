package com.myschool.notificationservice.dto;

import lombok.*;

/**
 * DTO pour les données utilisateur récupérées via Feign depuis user-service.
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class UserDto {
    private Long id;
    private String email;
    private String fullName;
}
