package com.myschool.userservice.repository;

import com.myschool.userservice.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findAllByStudentId(Long studentId);
    List<Enrollment> findAllByCourseId(Long courseId);
    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);
    void deleteAllByStudentId(Long studentId);

    @Query("SELECT e.courseId, COUNT(e) FROM Enrollment e GROUP BY e.courseId")
    List<Object[]> countEnrollmentsGroupedByCourse();

    @Query("SELECT e.studentId, COUNT(e) FROM Enrollment e GROUP BY e.studentId")
    List<Object[]> countEnrollmentsGroupedByStudent();
}