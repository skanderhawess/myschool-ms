package com.myschool.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "students")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Student {

    @Id
    @Column(name = "id")
    private Long id; // same value as users.id

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "id") // students.id references users.id
    private User user;

    @Column(name = "level", nullable = false, length = 255)
    private String level;
}